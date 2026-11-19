/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiCompiledElement;
import com.intellij.psi.PsiDocCommentOwner;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.search.AllClassesSearchExecutor;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.javadoc.PsiDocTagValue;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.PsiShortNamesCache;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.InheritanceUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.PairProcessor;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.apache.grails.intellij.plugin.mvc.MvcModuleStructureSynchronizer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class GspTagLibUtil {

  private static final Set<String> NOT_TAGS =
    ContainerUtil.newHashSet("grailsApplication", "out", "grailsUrlMappingsHolder", "typeConverter", "renderNoSelectionOption");

  public static final @NonNls String DEFAULT_TAGLIB_PREFIX = "g";
  public static final @NonNls String DYNAMIC_TAGLIB_PACKAGE = "org.codehaus.groovy.grails.plugins.web.taglib";
  public static final @NonNls String NAMESPACE_FIELD = "namespace";

  private GspTagLibUtil() {
  }

  public static Set<String> getExcludedTags(String prefix) {
    return DEFAULT_TAGLIB_PREFIX.equals(prefix) ? NOT_TAGS : Collections.emptySet();
  }

  public static TagLibNamespaceDescriptor getTagLibClasses(@NotNull PsiElement place, @NotNull String tagLibPrefix) {
    return getTagLibClasses(place).get(tagLibPrefix);
  }

  public static Map<String, TagLibNamespaceDescriptor> getTagLibClasses(final @NotNull PsiElement place) {
    Module module = ModuleUtilCore.findModuleForPsiElement(place);
    if (module == null) return Collections.emptyMap();

    return getTagLibClasses(module);
  }

  public static Map<String, TagLibNamespaceDescriptor> getTagLibClasses(final @NotNull Module module) {
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(
      module,
      () -> CachedValueProvider.Result.create(
        computeCustomTaglibClasses(module),
        PsiModificationTracker.MODIFICATION_COUNT,
        // The taglib set is index-derived and degrades in dumb mode; expire it once indexing ends
        // so a partial namespace map cannot outlive indexing.
        DumbService.getInstance(module.getProject()).getModificationTracker()
      )
    );
  }

  private static Map<String, TagLibNamespaceDescriptor> computeCustomTaglibClasses(Module module) {
    final Map<String, TagLibNamespaceDescriptor> res = new HashMap<>();

    res.put(DEFAULT_TAGLIB_PREFIX, new TagLibNamespaceDescriptor(DEFAULT_TAGLIB_PREFIX, module)); // Maps key set must  contain default prefix!!!

    // Everything past this point needs indexes. Discovering the taglib classes is one thing, but
    // even reading a found class's `namespace` field goes through getPrefixByTagLibClass ->
    // findCodeFieldByName, which resolves supertypes and therefore queries the stub index. In dumb
    // mode that throws IndexNotReadyException, and this method sits under XmlTag.getNamespace(),
    // which the daemon calls on any GSP - so it must return something rather than fail.
    //
    // Degrading to the default "g" prefix keeps core GSP editing working while indexing; the
    // custom prefixes appear when getTagLibClasses() recomputes on the DumbService tracker.
    if (DumbService.isDumb(module.getProject())) return res;

    final Collection<GrTypeDefinition> taglibs = new LinkedHashSet<>();
    taglibs.addAll(GrailsArtifact.TAGLIB.getInstances(module).values());
    taglibs.addAll(searchTaglibsInClassPath(module));

    for (GrTypeDefinition classDefinition : taglibs) {
      String prefix = getPrefixByTagLibClass(classDefinition);

      TagLibNamespaceDescriptor descr = res.get(prefix);
      if (descr == null) {
        descr = new TagLibNamespaceDescriptor(prefix, module);
        res.put(prefix, descr);
      }

      descr.addClass(classDefinition);
    }

    PsiPackage psiPackage = JavaPsiFacade.getInstance(module.getProject()).findPackage(DYNAMIC_TAGLIB_PACKAGE);
    if (psiPackage != null) {
      for (PsiClass tagLibClass : psiPackage.getClasses(GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))) {
        String name = tagLibClass.getName();
        if (name != null && name.endsWith(GrailsArtifact.TAGLIB.suffix)) {
          String prefix = TAGLIB_PREFEXES.get(tagLibClass.getQualifiedName());
          if (prefix == null) {
            prefix = DEFAULT_TAGLIB_PREFIX;
          }

          TagLibNamespaceDescriptor descr = res.get(prefix);
          if (descr == null) {
            descr = new TagLibNamespaceDescriptor(prefix, module);
            res.put(prefix, descr);
          }

          descr.addClass(tagLibClass);
        }
      }
    }

    return res;
  }

  private static @NotNull Collection<GrTypeDefinition> searchTaglibsInClassPath(final @NotNull Module module) {
    final Project project = module.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(module, () -> CachedValueProvider.Result.create(
      doSearchTaglibsInClassPath(module),
      PsiModificationTracker.MODIFICATION_COUNT,
      MvcModuleStructureSynchronizer.getInstance(project).getFileAndRootsModificationTracker(),
      DumbService.getInstance(project).getModificationTracker()
    ));
  }

  private static @NotNull Collection<GrTypeDefinition> doSearchTaglibsInClassPath(final @NotNull Module module) {
    final Project project = module.getProject();

    // Scanning class names needs the stub index, which throws IndexNotReadyException in dumb mode.
    // Returning nothing leaves the source taglibs and the default "g" prefix in place, so GSP
    // editing degrades instead of failing; searchTaglibsInClassPath() expires this on smart mode.
    if (DumbService.isDumb(project)) return Collections.emptyList();

    final GlobalSearchScope scope = module.getModuleRuntimeScope(false);

    final List<String> taglibNames = new ArrayList<>();
    AllClassesSearchExecutor.processClassNames(project, scope, fqn -> {
      if (fqn.endsWith(GrailsArtifact.TAGLIB.suffix)) {
        taglibNames.add(fqn);
      }
      return true;
    });

    final PsiShortNamesCache namesCache = PsiShortNamesCache.getInstance(project);
    final Collection<GrTypeDefinition> taglibs = new LinkedHashSet<>();
    for (String name : taglibNames) {
      final PsiClass[] classes = namesCache.getClassesByName(name, scope);
      for (PsiClass clazz : classes) {
        if (!(clazz instanceof PsiCompiledElement)) continue;
        final PsiElement mirror = clazz.getNavigationElement();
        if (!(mirror instanceof GrTypeDefinition)) continue;
        taglibs.add((GrTypeDefinition)mirror);
      }
    }
    return taglibs;
  }

  private static final Map<String, String> TAG_2_FQN_1_2;
  private static final Map<String, String> TAG_2_FQN_3;
  private static final Map<String, String> FQN_2_TAG;

  static {
    Map<String, String> tag2Fqn12 = new HashMap<>(11);
    // List of these tags get from org.codehaus.groovy.grails.web.taglib.GrailsTagRegistry.instance. Check this list when grails releases. #CHECK#
    tag2Fqn12.put("renderInput", "org.codehaus.groovy.grails.web.taglib.RenderInputTag");
    tag2Fqn12.put("each", "org.codehaus.groovy.grails.web.taglib.GroovyEachTag");
    tag2Fqn12.put("if", "org.codehaus.groovy.grails.web.taglib.GroovyIfTag");
    tag2Fqn12.put("unless", "org.codehaus.groovy.grails.web.taglib.GroovyUnlessTag");
    tag2Fqn12.put("else", "org.codehaus.groovy.grails.web.taglib.GroovyElseTag");
    tag2Fqn12.put("elseif", "org.codehaus.groovy.grails.web.taglib.GroovyElseIfTag");
    tag2Fqn12.put("findAll", "org.codehaus.groovy.grails.web.taglib.GroovyFindAllTag");
    tag2Fqn12.put("collect", "org.codehaus.groovy.grails.web.taglib.GroovyCollectTag");
    tag2Fqn12.put("grep", "org.codehaus.groovy.grails.web.taglib.GroovyGrepTag");
    tag2Fqn12.put("while", "org.codehaus.groovy.grails.web.taglib.GroovyWhileTag");
    tag2Fqn12.put("def", "org.codehaus.groovy.grails.web.taglib.GroovyDefTag");

    Map<String, String> tag2Fqn3 = new HashMap<>(10);
    tag2Fqn3.put("each", "org.grails.gsp.compiler.tags.GroovyEachTag");
    tag2Fqn3.put("if", "org.grails.gsp.compiler.tags.GroovyIfTag");
    tag2Fqn3.put("unless", "org.grails.gsp.compiler.tags.GroovyUnlessTag");
    tag2Fqn3.put("else", "org.grails.gsp.compiler.tags.GroovyElseTag");
    tag2Fqn3.put("elseif", "org.grails.gsp.compiler.tags.GroovyElseIfTag");
    tag2Fqn3.put("findAll", "org.grails.gsp.compiler.tags.GroovyFindAllTag");
    tag2Fqn3.put("collect", "org.grails.gsp.compiler.tags.GroovyCollectTag");
    tag2Fqn3.put("grep", "org.grails.gsp.compiler.tags.GroovyGrepTag");
    tag2Fqn3.put("while", "org.grails.gsp.compiler.tags.GroovyWhileTag");
    tag2Fqn3.put("def", "org.grails.gsp.compiler.tags.GroovyDefTag");

    Map<String, String> fqn2Tag = new HashMap<>(tag2Fqn12.size() + tag2Fqn3.size());
    fqn2Tag.putAll(ContainerUtil.reverseMap(tag2Fqn12));
    fqn2Tag.putAll(ContainerUtil.reverseMap(tag2Fqn3));

    TAG_2_FQN_1_2 = tag2Fqn12;
    TAG_2_FQN_3 = tag2Fqn3;
    FQN_2_TAG = fqn2Tag;
  }

  /**
   * This is list of standard TagLib classes which have namespace different from 'g'
   * Standard TagLib classes are a TagLib classes from package 'org.codehaus.groovy.grails.plugins.web.taglib' (see computeCustomTaglibClasses() method)
   * Check this list when grails releases. #CHECK#
   */
  public static final Map<String, String> TAGLIB_PREFEXES = new HashMap<>();
  static {
    TAGLIB_PREFEXES.put("org.codehaus.groovy.grails.plugins.web.taglib.PluginTagLib", "plugin");
    TAGLIB_PREFEXES.put("org.codehaus.groovy.grails.plugins.web.taglib.SitemeshTagLib", "sitemesh");
  }

  private static final String TAG_ANNOTATION = "grails.gsp.Tag";

  private static final String NOT_A_TAG_ANNOTATION = "grails.gsp.NotATag";

  private static final String ATTRS_PARAMETER_NAME = "attrs";

  private static final String BODY_PARAMETER_NAME = "body";

  /** Names that are Groovy or Object plumbing, or that every tag library has through the framework traits. */
  private static final Set<String> NON_TAG_METHOD_NAMES = Set.of(
    "invokeMethod", "methodMissing", "propertyMissing", "getProperty", "setProperty", "getMetaClass", "setMetaClass", "equals",
    "hashCode", "toString",
    "afterPropertiesSet", "currentRequestAttributes", "destroy", "initializeTagLibrary", "onApplicationEvent", "raw",
    "throwTagError", "withCodec");

  /**
   * Whether the Grails of a module dispatches tags to methods, which Grails does from 8.0 on. Before that, a method of a
   * tag library is an ordinary helper, whatever its shape. The {@code grails.gsp.Tag} annotation came with method tags,
   * and looking for it works for every kind of project, where the Grails version is not always known.
   */
  public static boolean supportsMethodTags(@NotNull Module module) {
    Project project = module.getProject();
    return CachedValuesManager.getManager(project).getCachedValue(module, () -> CachedValueProvider.Result.create(
      !DumbService.isDumb(project) &&
      JavaPsiFacade.getInstance(project).findClass(TAG_ANNOTATION, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)) != null,
      PsiModificationTracker.MODIFICATION_COUNT,
      ProjectRootModificationTracker.getInstance(project),
      DumbService.getInstance(project).getModificationTracker()
    ));
  }

  /**
   * The methods of a tag library class that are tags, which Grails 8 lets a tag library declare besides Closure fields.
   * Only the methods the class itself declares count: Grails dispatches a method tag from the declared methods, where it
   * finds a closure tag as an inherited property. Whether the project's Grails dispatches tags to methods at all is for the
   * caller to check, with {@link #supportsMethodTags}.
   */
  public static @NotNull List<PsiMethod> getTagMethods(@NotNull PsiClass tagLibClass) {
    return ContainerUtil.filter(getDeclaredMethods(tagLibClass), GspTagLibUtil::isTagMethod);
  }

  /**
   * The methods that together are the tag a tag method declares: the tag methods of its name that its class declares.
   * Grails tries each of them when a page calls the tag, the one with the most parameters first
   * (org.grails.taglib.TagMethodInvoker).
   */
  public static @NotNull List<PsiMethod> getTagOverloads(@NotNull PsiMethod tagMethod) {
    PsiClass tagLibClass = tagMethod.getContainingClass();
    if (tagLibClass == null) return List.of(tagMethod);

    List<PsiMethod> overloads = ContainerUtil.filter(getDeclaredMethods(tagLibClass), method ->
      method.getName().equals(tagMethod.getName()) && isTagMethod(method));
    return overloads.isEmpty() ? List.of(tagMethod) : overloads;
  }

  private static PsiMethod @NotNull [] getDeclaredMethods(@NotNull PsiClass aClass) {
    return aClass instanceof GrTypeDefinition definition ? definition.getCodeMethods() : aClass.getMethods();
  }

  /**
   * Whether a method of a tag library is a tag, by the rules of org.grails.taglib.discovery.TagDiscoveryRules, which
   * Grails 8 applies both when it compiles a tag library and when it runs one. #CHECK#
   */
  public static boolean isTagMethod(@NotNull PsiMethod method) {
    if (method.isConstructor() || !method.hasModifierProperty(PsiModifier.PUBLIC) ||
        method.hasModifierProperty(PsiModifier.STATIC) || method.hasModifierProperty(PsiModifier.ABSTRACT)) {
      return false;
    }
    if (method.hasAnnotation(NOT_A_TAG_ANNOTATION)) return false;
    if (method.hasAnnotation(TAG_ANNOTATION)) return true;

    String name = method.getName();
    if (name.isEmpty() || name.charAt(0) == '<' || name.indexOf('$') >= 0 || NON_TAG_METHOD_NAMES.contains(name)) return false;

    PsiParameter[] parameters = method.getParameterList().getParameters();
    // Property accessors are never tags.
    if (parameters.length == 0 && (name.startsWith("get") || name.startsWith("is"))) return false;
    if (parameters.length == 1 && name.startsWith("set")) return false;

    // A tag can be called as (attrs), (body) or (attrs, body), counting the shorter calls default values allow.
    int required = 0;
    for (PsiParameter parameter : parameters) {
      if (!(parameter instanceof GrParameter grParameter && grParameter.isOptional())) required++;
    }
    for (int arity = Math.max(required, 1); arity <= parameters.length; arity++) {
      if (arity == 1 && (isAttrsParameter(parameters[0]) || isBodyParameter(parameters[0]))) return true;
      if (arity == 2 && isAttrsParameter(parameters[0]) && isBodyParameter(parameters[1])) return true;
    }
    return false;
  }

  /** Whether a method tag takes a body, which only a Closure parameter of one of its overloads can supply. */
  public static boolean acceptsBody(@NotNull PsiMethod tagMethod) {
    return ContainerUtil.exists(getTagOverloads(tagMethod), overload ->
      ContainerUtil.exists(overload.getParameterList().getParameters(), GspTagLibUtil::isBodyParameter));
  }

  private static boolean isAttrsParameter(@NotNull PsiParameter parameter) {
    return InheritanceUtil.isInheritor(parameter.getType(), CommonClassNames.JAVA_UTIL_MAP) &&
           carriesName(parameter, ATTRS_PARAMETER_NAME);
  }

  private static boolean isBodyParameter(@NotNull PsiParameter parameter) {
    return InheritanceUtil.isInheritor(parameter.getType(), GroovyCommonClassNames.GROOVY_LANG_CLOSURE) &&
           carriesName(parameter, BODY_PARAMETER_NAME);
  }

  /** A compiled method may not have kept its parameter names, and then there is no name to check. */
  private static boolean carriesName(@NotNull PsiParameter parameter, @NotNull String expectedName) {
    return parameter instanceof PsiCompiledElement || expectedName.equals(parameter.getName());
  }

  public static boolean isSdkTagLib(@NotNull PsiClass aClass) {
    String name = aClass.getQualifiedName();
    return FQN_2_TAG.containsKey(name) ||
           DYNAMIC_TAGLIB_PACKAGE.equals(ClassUtil.extractPackageName(name));
  }

  public static @Nullable String getTagNameByClass(@NotNull String className) {
    return FQN_2_TAG.get(className);
  }

  public static @NotNull String getPrefixByTagLibClass(GrTypeDefinition clazz) {
    PsiField field = clazz.findCodeFieldByName(NAMESPACE_FIELD, true);
    GrField grField = null;
    if (field instanceof GrField) {
      grField = (GrField)field;
    }
    else if (field != null) {
      PsiElement source = field.getNavigationElement();
      if (source instanceof GrField) {
        grField = (GrField)source;
      }
    }
    if (grField == null) return DEFAULT_TAGLIB_PREFIX;
    if (grField.hasModifierProperty(PsiModifier.STATIC)) {
      GrExpression initializer = grField.getInitializerGroovy();
      if (initializer != null) {
        final Object value = JavaPsiFacade.getInstance(clazz.getProject()).getConstantEvaluationHelper().computeConstantExpression(initializer);
        if (value instanceof String) {
          String res = ((String)value).trim();
          return res.isEmpty() ? DEFAULT_TAGLIB_PREFIX : res;
        }
      }
    }
    return DEFAULT_TAGLIB_PREFIX;
  }

  private static @NotNull Map<String, String> getTag2Fqn(@NotNull PsiElement place) {
    GrailsApplication application = GrailsApplicationManager.findApplication(place);
    return application == null || application.getGrailsVersion().isLessThan(Version.GRAILS_3_0) ? TAG_2_FQN_1_2 : TAG_2_FQN_3;
  }

  public static void processBuiltInTagClasses(@Nullable String tagName,
                                              @NotNull PsiElement place,
                                              @NotNull PairProcessor<? super String, ? super PsiClass> processor) {
    Map<String, String> tag2Fqn = getTag2Fqn(place);
    if (tagName == null) {
      processBuiltInTagClasses(tag2Fqn, place, processor);
    }
    else {
      PsiClass builtInClass = getBuiltInTagByName(tag2Fqn, tagName, place);
      if (builtInClass == null) {
        return;
      }
      processor.process(tagName, builtInClass);
    }
  }

  private static @Nullable PsiClass getBuiltInTagByName(@NotNull Map<String, String> tag2Fqn,
                                                        @NotNull String tagName,
                                                        @NotNull PsiElement place) {
    String fqn = tag2Fqn.get(tagName);
    if (fqn == null) {
      return null;
    }
    final PsiClass builtInClass = JavaPsiFacade.getInstance(place.getProject()).findClass(fqn, place.getResolveScope());
    if (builtInClass != null) {
      return builtInClass;
    }
    return null;
  }

  private static void processBuiltInTagClasses(@NotNull Map<String, String> tag2Fqn,
                                               @NotNull PsiElement place,
                                               @NotNull PairProcessor<? super String, ? super PsiClass> processor) {
    final JavaPsiFacade facade = JavaPsiFacade.getInstance(place.getProject());
    final GlobalSearchScope resolveScope = place.getResolveScope();
    for (Map.Entry<String, String> entry : tag2Fqn.entrySet()) {
      final PsiClass builtInClass = facade.findClass(entry.getValue(), resolveScope);
      if (builtInClass != null && !processor.process(entry.getKey(), builtInClass)) {
        return;
      }
    }
  }

  public static boolean processGrailsTags(PsiScopeProcessor processor,
                                          PsiElement ref,
                                          ResolveState state,
                                          @Nullable String name,
                                          @Nullable ElementClassHint classHint) {
    return processGrailsTags(processor, ref, state, name, classHint, getTagLibClasses(ref));
  }

  public static boolean processGrailsTags(PsiScopeProcessor processor,
                                          PsiElement ref,
                                          ResolveState state,
                                          @Nullable String name,
                                          @Nullable ElementClassHint classHint,
                                          Map<String, TagLibNamespaceDescriptor> tagClassMap) {

    if (ResolveUtil.shouldProcessProperties(classHint)) {
      // process tags prefixes
      if (name == null) {
        for (TagLibNamespaceDescriptor descriptor : tagClassMap.values()) {
          PsiVariable variable = descriptor.getDummyClassVariable();
          if (variable != null && !processor.execute(variable, ResolveState.initial())) return false;
        }
      }
      else {
        if (ref.getParent() instanceof GrReferenceExpression) {
          TagLibNamespaceDescriptor descriptor = tagClassMap.get(name);
          if (descriptor != null) {
            PsiVariable variable = descriptor.getDummyClassVariable();
            if (variable != null && !processor.execute(variable, ResolveState.initial())) return false;
          }
        }
      }
    }

    if (ResolveUtil.shouldProcessMethods(classHint)) {
      TagLibNamespaceDescriptor descriptor = tagClassMap.get(DEFAULT_TAGLIB_PREFIX);
      if (descriptor != null) {
        if (!descriptor.processTags(processor, state, name)) return false;
      }
    }

    return true;
  }

  public static Pair<Map<String, XmlAttributeDescriptor>, Set<String>> getAttributesDescriptorsFromJavadocs(PsiElement place) {
    PsiElement element = place.getNavigationElement().getNavigationElement();
    return CachedValuesManager.getCachedValue(element, () -> {
      // A method tag can be declared as several overloads, and the tag's attributes are those of all of them.
      List<? extends PsiElement> declarations =
        element instanceof PsiMethod method && isTagMethod(method) ? getTagOverloads(method) : List.of(element);

      Map<String, XmlAttributeDescriptor> javaDocAttrMap = new HashMap<>();
      for (PsiElement declaration : declarations) {
        PsiDocComment docComment = declaration instanceof PsiDocCommentOwner owner ? owner.getDocComment() : null;
        if (docComment == null) continue;

        for (PsiDocTag attr : docComment.findTagsByName("attr")) {
          XmlAttributeDescriptor descriptor = createDescriptorByDoc(attr);
          if (descriptor != null) {
            javaDocAttrMap.put(descriptor.getName(), descriptor);
          }
        }
      }

      if (javaDocAttrMap.isEmpty()) {
        javaDocAttrMap = Collections.emptyMap();
      }

      Set<String> sourceAttrName = new HashSet<>();

      for (PsiElement declaration : declarations) {
        // The attributes the tag reads from its attrs map, apart from a few names.
        Collection<String> attrsMapNames = Collections.emptySet();
        if (declaration instanceof GrField field) {
          attrsMapNames = field.getNamedParameters().keySet();
        }
        else if (declaration instanceof GrMethod method && isTagMethod(method)) {
          attrsMapNames = method.getNamedParameters().keySet();
        }
        for (String attrName : attrsMapNames) {
          if (!"tagName".equals(attrName) && !"type".equals(attrName) && !"remove".equals(attrName)) sourceAttrName.add(attrName);
        }

        // For a method tag, also the parameters Grails 8 binds to the attribute of their name, whatever that name is
        // (org.grails.taglib.TagMethodInvoker).
        if (declaration instanceof GrMethod method && isTagMethod(method)) {
          for (PsiParameter parameter : method.getParameterList().getParameters()) {
            if (!isAttrsParameter(parameter) && !isBodyParameter(parameter)) sourceAttrName.add(parameter.getName());
          }
        }
      }

      sourceAttrName.removeAll(javaDocAttrMap.keySet());
      if (sourceAttrName.isEmpty()) {
        sourceAttrName = Collections.emptySet();
      }

      return CachedValueProvider.Result.create(Pair.create(javaDocAttrMap, sourceAttrName), element);
    });
  }

  private static @Nullable XmlAttributeDescriptor createDescriptorByDoc(final PsiDocTag docTag) {
    final PsiDocTagValue valueElement = docTag.getValueElement();
    if (valueElement == null) return null;

    String name = valueElement.getText().trim();
    if (name.isEmpty()) return null;

    PsiElement[] dataElements = docTag.getDataElements();
    final boolean isRequired = dataElements.length > 1
                               && StringUtil.startsWithIgnoreCase(dataElements[1].getText().trim(), "required");

    return new AnyXmlAttributeDescriptor(name) {
      @Override
      public boolean isRequired() {
        return isRequired;
      }

      @Override
      public PsiElement getDeclaration() {
        return valueElement;
      }
    };
  }

}

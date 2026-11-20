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

package org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.roots.ProjectRootModificationTracker;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.HierarchicalMethodSignature;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassInitializer;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiIdentifier;
import com.intellij.psi.PsiJavaToken;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiReferenceList;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.PsiTypeParameterList;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.ResolveState;
import com.intellij.psi.impl.InheritanceImplUtil;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.javadoc.PsiDocComment;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspClass;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspDeclarationHolder;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspRunMethod;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GspGroovyFile;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspFile;
import org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.apache.grails.intellij.plugin.util.version.VersionImpl;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiElementImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.PsiImplUtil;
import org.jetbrains.plugins.groovy.lang.resolve.CollectClassMembersUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class GrGspClassImpl extends GroovyPsiElementImpl implements GrGspClass {

  /** The class every compiled page extends, from Grails 3 on. */
  private static final String GROOVY_PAGE = "org.grails.gsp.GroovyPage";

  /** The same class before Grails 3 moved Grails out of {@code org.codehaus.groovy.grails}. */
  private static final String GRAILS_2_GROOVY_PAGE = "org.codehaus.groovy.grails.web.pages.GroovyPage";

  /**
   * The request wrapper the implicit variables come from. Its Grails 3 package means the classpath has the Grails 3
   * web API, so the variables take their types from {@link #GRAILS_3_VARIABLE_TYPES}.
   */
  private static final String GRAILS_3_WEB_REQUEST = "org.grails.web.servlet.mvc.GrailsWebRequest";

  private static final String JAVAX_SERVLET_PACKAGE = "javax.servlet";

  /**
   * The servlet API package from Jakarta EE 9 on, which Grails 7 is built on. Earlier Grails versions use
   * {@link #JAVAX_SERVLET_PACKAGE}.
   */
  private static final String JAKARTA_SERVLET_PACKAGE = "jakarta.servlet";

  private static final String JAKARTA_HTTP_SERVLET_REQUEST = JAKARTA_SERVLET_PACKAGE + ".http.HttpServletRequest";

  // @see org.codehaus.groovy.grails.web.pages.GroovyPageWritable#writeTo
  // and org.codehaus.groovy.grails.web.pages.GroovyPageRequestBinding#lazyRequestBasedValuesMap #CHECK#
  private static final Map<String, String> GRAILS_2_VARIABLE_TYPES = Map.of(
    "applicationContext", "org.codehaus.groovy.grails.commons.spring.GrailsWebApplicationContext",
    "flash", "org.codehaus.groovy.grails.web.servlet.FlashScope",
    "grailsApplication", "org.codehaus.groovy.grails.commons.GrailsApplication",
    "out", "org.codehaus.groovy.grails.web.util.GrailsPrintWriter",
    "params", "org.codehaus.groovy.grails.web.servlet.mvc.GrailsParameterMap",
    "webRequest", "org.codehaus.groovy.grails.web.servlet.mvc.GrailsWebRequest",
    "exception", "org.codehaus.groovy.grails.web.errors.GrailsWrappedRuntimeException");

  // Unchanged from Grails 3.3 to 8. There is no `out`: GroovyPage.getOut() declares it, and that getter wins over
  // a field here, as it already does in Grails 2.
  // @see org.grails.web.taglib.WebRequestTemplateVariableBinding#lazyRequestBasedValuesMap
  // and org.grails.web.errors.GrailsExceptionResolver, which binds `exception` as a request attribute #CHECK#
  private static final Map<String, String> GRAILS_3_VARIABLE_TYPES = Map.of(
    "applicationContext", "org.springframework.context.ApplicationContext",
    "flash", "grails.web.mvc.FlashScope",
    "grailsApplication", "grails.core.GrailsApplication",
    "params", "grails.web.servlet.mvc.GrailsParameterMap",
    "webRequest", GRAILS_3_WEB_REQUEST,
    "exception", "org.grails.web.errors.GrailsWrappedRuntimeException");

  /**
   * The first Grails release that binds {@code namespace}, the controller namespace. Its GSP library, 6.2.2, is the
   * first to bind it.
   */
  private static final Version NAMESPACE_VARIABLE_SINCE = new VersionImpl("6.2.1");

  /** The first Grails release that binds {@code servletContext}, the same object as {@code application}. */
  private static final Version SERVLET_CONTEXT_VARIABLE_SINCE = new VersionImpl("8.0.0-M6");

  /**
   * The Java source of a class whose fields are the implicit variables of a page.
   *
   * @param grailsTypes    the types of the variables whose classes moved between Grails versions
   * @param servletPackage the servlet API package, either {@link #JAVAX_SERVLET_PACKAGE} or {@link #JAKARTA_SERVLET_PACKAGE}
   * @param grailsVersion  the application's Grails version, which decides the variables later releases added, or
   *                       {@code null} when the page belongs to no application
   */
  private static @NotNull String implicitVariablesSource(@NotNull Map<String, String> grailsTypes,
                                                         @NotNull String servletPackage,
                                                         @Nullable Version grailsVersion) {
    // Sorted, so the same variables always produce the same source.
    Map<String, String> types = new TreeMap<>(grailsTypes);
    types.put("actionName", "String");
    types.put("controllerName", "String");
    types.put("pageScope", "groovy.lang.Binding");
    types.put("application", servletPackage + ".ServletContext");
    types.put("request", servletPackage + ".http.HttpServletRequest");
    types.put("response", servletPackage + ".http.HttpServletResponse");
    types.put("session", servletPackage + ".http.HttpSession");
    if (grailsVersion != null && grailsVersion.isAtLeast(NAMESPACE_VARIABLE_SINCE)) {
      types.put("namespace", "String");
    }
    if (grailsVersion != null && grailsVersion.isAtLeast(SERVLET_CONTEXT_VARIABLE_SINCE)) {
      types.put("servletContext", servletPackage + ".ServletContext");
    }

    StringBuilder source = new StringBuilder("class GspProperties {");
    types.forEach((name, type) -> source.append(" private final ").append(type).append(' ').append(name).append(';'));
    return source.append(" }").toString();
  }

  /**
   * The Grails API a page is compiled against, as far as this class models it.
   *
   * @param groovyPageClass         the fully qualified name of the class the page extends
   * @param implicitVariablesSource the source passed to {@link DynamicMemberUtils} for the implicit variables
   */
  private record PageApi(@NotNull String groovyPageClass, @NotNull String implicitVariablesSource) {
  }

  public GrGspClassImpl(ASTNode node) {
    super(node);
  }

  @Override
  public String toString() {
    return "GspClass";
  }

  //copy-paste from Grails (GroovyPageCompiler)
  private static String generateJavaName(String str) {
    StringBuilder sb = new StringBuilder();
    int i = 0;
    boolean nextMustBeStartChar = true;
    while (i < str.length()) {
      char ch = str.charAt(i++);
      if (ch == '/') {
        nextMustBeStartChar = true;
        sb.append(ch);
      }
      else {
        // package or class name cannot start with a number
        if (nextMustBeStartChar && !Character.isJavaIdentifierStart(ch)) {
          sb.append('_');
        }
        nextMustBeStartChar = false;
        sb.append(Character.isJavaIdentifierPart(ch) ? ch : '_');
      }
    }
    return sb.toString();
  }


  @Override
  public @Nullable @NonNls String getQualifiedName() {
    return "gsp.default." + getName();
  }

  @Override
  public boolean isInterface() {
    return false;
  }

  @Override
  public boolean isAnnotationType() {
    return false;
  }

  @Override
  public boolean isEnum() {
    return false;
  }

  @Override
  public @Nullable PsiReferenceList getExtendsList() {
    return null;
  }

  @Override
  public @Nullable PsiReferenceList getImplementsList() {
    return null;
  }

  @Override
  public PsiClassType @NotNull [] getExtendsListTypes() {
    return PsiClassType.EMPTY_ARRAY;
  }

  @Override
  public PsiClassType @NotNull [] getImplementsListTypes() {
    return PsiClassType.EMPTY_ARRAY;
  }

  @Override
  public @Nullable PsiClass getSuperClass() {
    return JavaPsiFacade.getInstance(getProject()).findClass(getPageApi().groovyPageClass(), getResolveScope());
  }

  @Override
  public PsiClass @NotNull [] getInterfaces() {
    return PsiClass.EMPTY_ARRAY;
  }

  @Override
  public PsiClass @NotNull [] getSupers() {
    final PsiClass superClass = getSuperClass();
    return superClass != null ? new PsiClass[]{superClass} : PsiClass.EMPTY_ARRAY;
  }

  @Override
  public PsiClassType @NotNull [] getSuperTypes() {
    PsiElementFactory factory = JavaPsiFacade.getInstance(getProject()).getElementFactory();
    return new PsiClassType[]{factory.createTypeByFQClassName(getPageApi().groovyPageClass(), getResolveScope())};
  }

  @Override
  public GrField @NotNull [] getFields() {
    GrGspDeclarationHolder[] holders = getRunMethod().getRunBlock().getDeclarationHolders();
    ArrayList<GrField> fields = new ArrayList<>();
    for (GrGspDeclarationHolder holder : holders) {
      ContainerUtil.addAll(fields, holder.getFields());
    }
    return fields.toArray(GrField.EMPTY_ARRAY);
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor,
                                     @NotNull ResolveState state,
                                     PsiElement lastParent,
                                     @NotNull PsiElement place) {

    String name = ResolveUtil.getNameHint(processor);
    ElementClassHint classHint = processor.getHint(ElementClassHint.KEY);

    GspGroovyFile groovyFile = (GspGroovyFile)getContainingFile().getOriginalFile();

    GspFile gspFile = groovyFile.getGspLanguageRoot();

    if (ResolveUtil.shouldProcessProperties(classHint)) {
      // Process variables defined in tags like g:each
      if (!processGspVariables(processor, name, groovyFile, state, place)) return false;

      // Process variables returned from model.
      if (name == null) {
        GspModelVariableModel variableModel = GspModelVariableModel.getInstance(gspFile);
        for (String argumenName : variableModel.getArgumentNames()) {
          PsiVariable variable = variableModel.getVariable(argumenName);
          if (variable != null) {
            if (!processor.execute(variable, state)) return false;
          }
        }
      }
      else {
        PsiVariable variable = GspModelVariableModel.getInstance(gspFile).getVariable(name);
        if (variable != null) {
          if (!processor.execute(variable, state)) return false;
        }
      }

      // Process builtin gsp properties. (e.g. request, response, ...)
      for (PsiField field : DynamicMemberUtils.getMembers(place.getProject(), getPageApi().implicitVariablesSource()).getFields(name)) {
        if (!processor.execute(field, state)) return false;
      }
    }

    if (!GspTagLibUtil.processGrailsTags(processor, place, state, name, classHint)) return false;

    PsiClass superClass = getSuperClass();
    if (superClass != null) {
      if (!ResolveUtil.processClassDeclarations(superClass, processor, state, lastParent, place)) return false;
    }

    return true;
  }

  /**
   * Picks the Grails API from the classes on the page's classpath rather than from the Grails version, so a page
   * gets types that resolve. Each choice follows the class it is about: the base class is whichever
   * {@code GroovyPage} exists, the Grails types follow {@code GrailsWebRequest}, and the servlet types follow
   * {@code HttpServletRequest}. The newer API wins when both are present, which happens when an older library pulls
   * the javax servlet API in transitively.
   * <p>
   * The exception is the variables later releases added, {@code namespace} and {@code servletContext}: no class marks
   * them, so they follow the application's Grails version.
   */
  private @NotNull PageApi getPageApi() {
    return CachedValuesManager.getCachedValue(this, () -> {
      JavaPsiFacade facade = JavaPsiFacade.getInstance(getProject());
      GlobalSearchScope scope = getResolveScope();
      String groovyPageClass = facade.findClass(GROOVY_PAGE, scope) != null ? GROOVY_PAGE : GRAILS_2_GROOVY_PAGE;
      Map<String, String> grailsTypes =
        facade.findClass(GRAILS_3_WEB_REQUEST, scope) != null ? GRAILS_3_VARIABLE_TYPES : GRAILS_2_VARIABLE_TYPES;
      String servletPackage =
        facade.findClass(JAKARTA_HTTP_SERVLET_REQUEST, scope) != null ? JAKARTA_SERVLET_PACKAGE : JAVAX_SERVLET_PACKAGE;
      GrailsApplication application = GrailsApplicationManager.findApplication(this);
      Version grailsVersion = application == null ? null : application.getGrailsVersion();
      PageApi api = new PageApi(groovyPageClass, implicitVariablesSource(grailsTypes, servletPackage, grailsVersion));
      return CachedValueProvider.Result.create(api,
                                               PsiModificationTracker.MODIFICATION_COUNT,
                                               ProjectRootModificationTracker.getInstance(getProject()),
                                               // Recomputed applications can have another Grails version.
                                               GrailsApplicationManager.getInstance(getProject()),
                                               // The lookups can miss classes while indexing; drop those answers
                                               // when indexing ends.
                                               DumbService.getInstance(getProject()).getModificationTracker());
    });
  }

  private static boolean processGspVariables(final @NotNull PsiScopeProcessor processor,
                                             @Nullable String name,
                                             @NotNull GspGroovyFile file,
                                             @NotNull ResolveState state,
                                             @NotNull PsiElement place) {
    if (place.getContainingFile().getOriginalFile() != file) return true;
    if (!file.getSkeleton().processElementAtOffset(place.getTextOffset(), processor, name, state)) return false;

    return true;
  }

  @Override
  public GrMethod @NotNull [] getMethods() {
    GrGspDeclarationHolder[] holders = getRunMethod().getRunBlock().getDeclarationHolders();
    ArrayList<GrMethod> methods = new ArrayList<>();
    for (GrGspDeclarationHolder holder : holders) {
      ContainerUtil.addAll(methods, holder.getMethods());
    }
    return methods.toArray(GrMethod.EMPTY_ARRAY);
  }

  @Override
  public PsiMethod @NotNull [] getConstructors() {
    return PsiMethod.EMPTY_ARRAY;
  }

  @Override
  public PsiClass @NotNull [] getInnerClasses() {
    return PsiClass.EMPTY_ARRAY;
  }

  @Override
  public PsiClassInitializer @NotNull [] getInitializers() {
    return PsiClassInitializer.EMPTY_ARRAY;
  }

  @Override
  public PsiField @NotNull [] getAllFields() {
    return PsiField.EMPTY_ARRAY;
  }

  @Override
  public PsiMethod @NotNull [] getAllMethods() {
    return PsiMethod.EMPTY_ARRAY;
  }

  @Override
  public PsiClass @NotNull [] getAllInnerClasses() {
    return PsiClass.EMPTY_ARRAY;
  }

  @Override
  public @Nullable PsiField findFieldByName(String name, boolean checkBases) {
    if (!checkBases) {
      for (GrField field : getFields()) {
        if (name.equals(field.getName())) return field;
      }
      return null;
    }
    Map<String, CandidateInfo> fieldsMap = CollectClassMembersUtil.getAllFields(this);
    final CandidateInfo info = fieldsMap.get(name);
    return info == null ? null : (PsiField) info.getElement();
  }

  @Override
  public @Nullable PsiMethod findMethodBySignature(@NotNull PsiMethod patternMethod, boolean checkBases) {
    return null;
  }

  @Override
  public PsiMethod @NotNull [] findMethodsBySignature(@NotNull PsiMethod patternMethod, boolean checkBases) {
    return PsiMethod.EMPTY_ARRAY;
  }

  @Override
  public PsiMethod @NotNull [] findMethodsByName(@NonNls String name, boolean checkBases) {
    if (!checkBases) {
      List<PsiMethod> result = new ArrayList<>();
      for (GrMethod method : getMethods()) {
        if (name.equals(method.getName())) result.add(method);
      }

      return result.toArray(PsiMethod.EMPTY_ARRAY);
    }

    Map<String, List<CandidateInfo>> methodsMap = CollectClassMembersUtil.getAllMethods(this, true);
    return PsiImplUtil.mapToMethods(methodsMap.get(name));
  }

  @Override
  public @NotNull List<Pair<PsiMethod, PsiSubstitutor>> findMethodsAndTheirSubstitutorsByName(@NotNull String name, boolean checkBases) {
    return Collections.emptyList();
  }

  @Override
  public @NotNull List<Pair<PsiMethod, PsiSubstitutor>> getAllMethodsAndTheirSubstitutors() {
    return Collections.emptyList();
  }

  @Override
  public @Nullable PsiClass findInnerClassByName(String name, boolean checkBases) {
    return null;
  }

  @Override
  public @Nullable PsiJavaToken getLBrace() {
    return null;
  }

  @Override
  public @Nullable PsiJavaToken getRBrace() {
    return null;
  }

  @Override
  public @Nullable PsiIdentifier getNameIdentifier() {
    return null;
  }

  @Override
  public PsiElement getScope() {
    return null;
  }

  @Override
  public boolean isInheritor(@NotNull PsiClass baseClass, boolean checkDeep) {
    return InheritanceImplUtil.isInheritor(this, baseClass, checkDeep);
  }

  @Override
  public boolean isInheritorDeep(@NotNull PsiClass baseClass, @Nullable PsiClass classToByPass) {
    return InheritanceImplUtil.isInheritorDeep(this, baseClass, classToByPass);
  }

  @Override
  public @Nullable PsiClass getContainingClass() {
    return null;
  }

  @Override
  public @NotNull Collection<HierarchicalMethodSignature> getVisibleSignatures() {
    return Collections.emptyList();
  }

  @Override
  public @NotNull GrGspRunMethod getRunMethod() {
    GrGspRunMethod method = findChildByClass(GrGspRunMethod.class);
    assert method != null;
    return method;
  }

  @Override
  public PsiElement setName(@NonNls @NotNull String name) throws IncorrectOperationException {
    throw new IncorrectOperationException("There is no way to set up gsp class name");
  }

  @Override
  public @Nullable PsiModifierList getModifierList() {
    return null;
  }

  @Override
  public boolean hasModifierProperty(@NonNls @NotNull String name) {
    return false;
  }

  @Override
  public @Nullable PsiDocComment getDocComment() {
    return null;
  }

  @Override
  public boolean isDeprecated() {
    return false;
  }

  @Override
  public boolean hasTypeParameters() {
    return false;
  }

  @Override
  public @Nullable PsiTypeParameterList getTypeParameterList() {
    return null;
  }

  @Override
  public PsiTypeParameter @NotNull [] getTypeParameters() {
    return PsiTypeParameter.EMPTY_ARRAY;
  }

  @Override
  public String getName() {
    final VirtualFile file = getContainingFile().getOriginalFile().getVirtualFile();
    assert file != null;
    String name = file.getName();

    final VirtualFile views = GrailsUtils.findViewsDirectory(this);
    if (views != null) {
      final String path = VfsUtilCore.getRelativePath(file, views, '/');
      if (path != null) {
        name = path;
      }
    }

    return generateJavaName(name);
  }

}

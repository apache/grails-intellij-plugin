/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.grails.intellij.plugin.artefact.impl;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.searches.AllClassesSearch;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.CachedValue;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.util.containers.FactoryMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.artefact.api.ArtefactHandlers;
import org.apache.grails.intellij.plugin.artefact.api.GrailsArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.api.HandlerCache;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrAnnotationUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

public final class GrailsArtefacts {

  private static final String GRAILS_ARTEFACT_ARTEFACT = "grails.artefact.Artefact";

  /** One cache key per handler, so each handler's artefacts are cached independently. */
  private static final Map<GrailsArtefactHandler, Key<CachedValue<Map<GlobalSearchScope, Collection<PsiClass>>>>> KEYS =
    FactoryMap.create(handler -> Key.create("grails.artefact.cache." + handler.getClass().getName()));

  private GrailsArtefacts() {
  }

  public static @NotNull Collection<PsiClass> getArtefacts(@NotNull GrailsArtefactHandler handler,
                                                           @NotNull GrailsApplication application,
                                                           @NotNull GlobalSearchScope scope) {
    Map<GlobalSearchScope, Collection<PsiClass>> perScope = CachedValuesManager
      .getManager(application.getProject())
      .getCachedValue(application, KEYS.get(handler),
                      () -> Result.create(createCache(handler, application), PsiModificationTracker.MODIFICATION_COUNT),
                      false);
    Collection<PsiClass> result = perScope.get(scope);
    return result != null ? result : List.of();
  }

  private static @NotNull Map<GlobalSearchScope, Collection<PsiClass>> createCache(@NotNull GrailsArtefactHandler handler,
                                                                                   @NotNull GrailsApplication application) {
    return FactoryMap.create(scope -> ApplicationManager.getApplication()
      .runReadAction((com.intellij.openapi.util.Computable<Collection<PsiClass>>)() -> doGetArtefacts(handler, application, scope)));
  }

  private static @NotNull Collection<PsiClass> doGetArtefacts(@NotNull GrailsArtefactHandler handler,
                                                              @NotNull GrailsApplication application,
                                                              @NotNull GlobalSearchScope scope) {
    Collection<PsiClass> result = new LinkedHashSet<>();

    Collection<PsiClass> conventional = collectConventionalArtefacts(handler, application, scope);
    if (conventional != null) result.addAll(conventional);

    Collection<PsiClass> compiled = collectCompiledArtefacts(handler, application, scope);
    if (compiled != null) result.addAll(compiled);

    Collection<PsiClass> annotated = collectAnnotatedArtefacts(handler, application, scope);
    if (annotated != null) result.addAll(annotated);

    result.addAll(collectSpecificAnnotatedArtefacts(handler, application, scope));

    return result;
  }

  /**
   * PsiClass is considered a convention artefact when all the following us true:
   * - its name has specified suffix
   * - its name if the same as the name of file
   * - it is defined in a groovy file which lies under specified directory
   */
  private static @Nullable Collection<PsiClass> collectConventionalArtefacts(@NotNull GrailsArtefactHandler handler,
                                                                             @NotNull GrailsApplication application,
                                                                             @NotNull GlobalSearchScope scope) {
    Project project = application.getProject();
    VirtualFile directory = handler.getDirectory(application);
    if (directory == null) return null;

    GlobalSearchScope artefactDirectoryScope = GlobalSearchScopesCore.directoryScope(project, directory, true);
    GlobalSearchScope resultScope = artefactDirectoryScope.intersectWith(scope);
    String suffix = handler.getArtefactClassSuffix();

    List<PsiClass> result = new ArrayList<>();
    for (PsiClass artefactClass : AllClassesSearch.search(resultScope, project, className -> className.endsWith(suffix)).findAll()) {
      VirtualFile file = virtualFileOf(artefactClass);
      if (file != null
          && file.getNameWithoutExtension().equals(artefactClass.getName())
          && file.getFileType() == GroovyFileType.GROOVY_FILE_TYPE) {
        result.add(artefactClass);
      }
    }
    return result;
  }

  /**
   * Collects classes defined in META-INF/grails-plugin.xml
   */
  private static @Nullable Collection<PsiClass> collectCompiledArtefacts(@NotNull GrailsArtefactHandler handler,
                                                                         @NotNull GrailsApplication application,
                                                                         @NotNull GlobalSearchScope scope) {
    Project project = application.getProject();
    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
    PsiPackage metaInfPackage = facade.findPackage("META-INF");
    if (metaInfPackage == null) return null;

    String suffix = handler.getArtefactClassSuffix();
    List<PsiClass> result = new ArrayList<>();

    for (PsiDirectory directory : metaInfPackage.getDirectories(scope)) {
      if (!(directory.findFile("grails-plugin.xml") instanceof XmlFile file)) continue;
      XmlTag rootTag = file.getRootTag();
      if (rootTag == null) continue;
      XmlTag[] resourcesTags = rootTag.findSubTags("resources");
      if (resourcesTags.length == 0) continue;

      for (XmlTag tag : resourcesTags[0].getSubTags()) {
        if (!"resource".equals(tag.getName())) continue;
        String fqn = tag.getValue().getTrimmedText();
        if (fqn.endsWith(suffix)) {
          PsiClass found = facade.findClass(fqn, scope);
          if (found != null) result.add(found);
        }
      }
    }

    return result;
  }

  /**
   * Collects classes annotated with @Artefact("artefactHandlerId"), where artefactId corresponds to the current artefact handler.
   */
  private static @Nullable Collection<PsiClass> collectAnnotatedArtefacts(@NotNull GrailsArtefactHandler handler,
                                                                          @NotNull GrailsApplication application,
                                                                          @NotNull GlobalSearchScope scope) {
    Project project = application.getProject();
    GlobalSearchScope annotationClassSearchScope = application.getScope(true, false);

    PsiClass annotationClass = JavaPsiFacade.getInstance(project).findClass(GRAILS_ARTEFACT_ARTEFACT, annotationClassSearchScope);
    if (annotationClass == null) return null;

    String suffix = handler.getArtefactClassSuffix();
    List<PsiClass> result = new ArrayList<>();

    for (PsiClass artefactClass : AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope).findAll()) {
      VirtualFile file = virtualFileOf(artefactClass);
      if (file == null || file.getFileType() != GroovyFileType.GROOVY_FILE_TYPE) continue;

      String name = artefactClass.getName();
      if (name == null || !name.endsWith(suffix)) continue;

      PsiModifierList modifierList = artefactClass.getModifierList();
      if (modifierList == null) continue;
      PsiAnnotation annotation = modifierList.findAnnotation(GRAILS_ARTEFACT_ARTEFACT);
      if (annotation == null) continue;

      if (handler.getArtefactHandlerID().equals(GrAnnotationUtil.inferStringAttribute(annotation, "value"))) {
        result.add(artefactClass);
      }
    }

    return result;
  }

  /**
   * Collects classes annotated with artefact specific annotation, i.e. @Controller or @Taglib.
   */
  private static @NotNull Collection<PsiClass> collectSpecificAnnotatedArtefacts(@NotNull GrailsArtefactHandler handler,
                                                                                 @NotNull GrailsApplication application,
                                                                                 @NotNull GlobalSearchScope scope) {
    Project project = application.getProject();
    GlobalSearchScope annotationClassSearchScope = application.getScope(true, false);
    String suffix = handler.getArtefactClassSuffix();

    List<PsiClass> result = new ArrayList<>();
    for (String annotationFqn : handler.getAnnotationFqns()) {
      PsiClass annotationClass = JavaPsiFacade.getInstance(project).findClass(annotationFqn, annotationClassSearchScope);
      if (annotationClass == null) continue;

      for (PsiClass artefactClass : AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope).findAll()) {
        VirtualFile file = virtualFileOf(artefactClass);
        String name = artefactClass.getName();
        if (file != null
            && file.getFileType() == GroovyFileType.GROOVY_FILE_TYPE
            && name != null
            && name.endsWith(suffix)) {
          result.add(artefactClass);
        }
      }
    }
    return result;
  }

  public static @Nullable GrailsArtefactHandler getArtefactHandler(@NotNull PsiClass clazz) {
    if (!(clazz instanceof GrTypeDefinition typeDefinition)) return null;
    return CachedValuesManager.getCachedValue(typeDefinition,
                                              () -> Result.create(doGetArtefactHandler(typeDefinition), typeDefinition));
  }

  private static @Nullable GrailsArtefactHandler doGetArtefactHandler(@NotNull GrTypeDefinition clazz) {
    GrailsApplication application = GrailsApplicationManager.findApplication(clazz);
    if (application == null) return null;
    String name = clazz.getName();
    if (name == null) return null;

    HandlerCache handlerCache = ApplicationManager.getApplication().getService(HandlerCache.class);

    // first check for @Artefact
    PsiAnnotation artefactAnnotation = AnnotationUtil.findAnnotation(clazz, GRAILS_ARTEFACT_ARTEFACT);
    if (artefactAnnotation != null) {
      String id = GrAnnotationUtil.inferStringAttribute(artefactAnnotation, "value");
      if (id != null) {
        GrailsArtefactHandler handler = handlerCache.getIdToHandler().get(id);
        if (handler != null) return handler;
      }
    }

    // check for @TagLib, @Entity, etc
    for (Map.Entry<String, GrailsArtefactHandler> entry : handlerCache.getAnnotationToHandler().entrySet()) {
      if (AnnotationUtil.findAnnotation(clazz, entry.getKey()) != null) {
        return entry.getValue();
      }
    }

    VirtualFile file = virtualFileOf(clazz);
    if (file == null) return null;
    if (!file.getNameWithoutExtension().equals(name)) return null;

    // check if it's conventional artefact
    for (GrailsArtefactHandler handler : ArtefactHandlers.allHandlers()) {
      if (!name.endsWith(handler.getArtefactClassSuffix())) continue;

      VirtualFile artefactDirectory = handler.getDirectory(application);
      if (artefactDirectory == null) continue;
      if (!VfsUtilCore.isAncestor(artefactDirectory, file, true)) continue;

      return handler;
    }

    return null;
  }

  private static @Nullable VirtualFile virtualFileOf(@NotNull PsiClass clazz) {
    PsiFile containingFile = clazz.getContainingFile();
    return containingFile != null ? containingFile.getVirtualFile() : null;
  }
}

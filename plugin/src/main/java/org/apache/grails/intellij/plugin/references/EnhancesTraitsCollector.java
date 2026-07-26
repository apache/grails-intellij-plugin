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
package org.apache.grails.intellij.plugin.references;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.AnnotatedElementsSearch;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.util.containers.ConcurrentFactoryMap;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrAnnotationUtil;
import org.jetbrains.plugins.groovy.lang.psi.util.GrTraitUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

/**
 * Traits contributed to an artefact type by {@code @grails.artefact.Enhances}, collected once per
 * resolve scope and invalidated on any PSI change.
 */
public final class EnhancesTraitsCollector {

  private EnhancesTraitsCollector() {
  }

  private static final String ANNOTATION_FQN = "grails.artefact.Enhances";

  @ApiStatus.Internal
  public static @NotNull Collection<String> doGetEnhancesTraits(@NotNull PsiElement context, @NotNull String artefactType) {
    Collection<String> traits = getAllEnhancesTraits(context).get(artefactType);
    return traits == null ? Collections.emptyList() : traits;
  }

  private static @NotNull Map<String, Collection<String>> getAllEnhancesTraits(@NotNull PsiElement context) {
    Project project = context.getProject();
    ConcurrentMap<GlobalSearchScope, Map<String, Collection<String>>> byScope =
      CachedValuesManager.getManager(project).getCachedValue(project, () -> Result.create(
        ConcurrentFactoryMap.<GlobalSearchScope, Map<String, Collection<String>>>createMap(
          scope -> doFindTraits(project, scope)),
        PsiModificationTracker.MODIFICATION_COUNT));
    Map<String, Collection<String>> result = byScope.get(context.getResolveScope());
    return result == null ? Collections.emptyMap() : result;
  }

  private static @NotNull Map<String, Collection<String>> doFindTraits(@NotNull Project project,
                                                                      @NotNull GlobalSearchScope scope) {
    PsiClass annotationClass = JavaPsiFacade.getInstance(project).findClass(ANNOTATION_FQN, scope);
    if (annotationClass == null) return Collections.emptyMap();

    Map<String, Collection<String>> result = new HashMap<>();
    for (PsiClass clazz : AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope).findAll()) {
      if (!GrTraitUtil.isTrait(clazz)) continue;
      String traitFqn = clazz.getQualifiedName();
      if (traitFqn == null) continue;
      PsiAnnotation annotation = AnnotationUtil.findAnnotation(clazz, ANNOTATION_FQN);
      if (annotation == null) continue;
      List<String> artefactTypes = GrAnnotationUtil.getStringArrayValue(annotation, "value", false);
      for (String artefactType : artefactTypes) {
        result.computeIfAbsent(artefactType, k -> new ArrayList<>()).add(traitFqn);
      }
    }
    return result;
  }
}

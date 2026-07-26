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

@file:JvmName("EnhancesTraitsCollector")

package org.jetbrains.plugins.grails.references

import com.intellij.codeInsight.AnnotationUtil
import com.intellij.openapi.project.Project
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.ConcurrentFactoryMap
import org.jetbrains.plugins.groovy.lang.psi.impl.GrAnnotationUtil
import org.jetbrains.plugins.groovy.lang.psi.util.GrTraitUtil
import java.util.concurrent.ConcurrentMap

internal fun doGetEnhancesTraits(context: PsiElement, artefactType: String): Collection<String> {
  return getAllEnhancesTraits(context)[artefactType] ?: emptyList()
}

private fun getAllEnhancesTraits(context: PsiElement): Map<String, Collection<String>> {
  val project = context.project
  return CachedValuesManager.getManager(project).getCachedValue(project) {
    val map: ConcurrentMap<GlobalSearchScope, Map<String, Collection<String>>> = ConcurrentFactoryMap.createMap { doFindTraits(project, it) }
    Result.create(map, PsiModificationTracker.MODIFICATION_COUNT)
  }[context.resolveScope] ?: emptyMap()
}

private const val annotationFqn = "grails.artefact.Enhances"

private fun doFindTraits(project: Project, scope: GlobalSearchScope): Map<String, Collection<String>> {
  val annotationClass = JavaPsiFacade.getInstance(project).findClass(annotationFqn, scope) ?: return emptyMap()
  val annotatedClasses = AnnotatedElementsSearch.searchPsiClasses(annotationClass, scope)
  val result = mutableMapOf<String, MutableCollection<String>>()
  for (clazz in annotatedClasses.findAll()) {
    if (!GrTraitUtil.isTrait(clazz)) continue
    val traitFqn = clazz.qualifiedName ?: continue
    val annotation = AnnotationUtil.findAnnotation(clazz, annotationFqn) ?: continue
    val artefactTypes = GrAnnotationUtil.getStringArrayValue(annotation, "value", false)
    for (artefactType in artefactTypes) {
      result.getOrPut(artefactType, { mutableListOf() }).add(traitFqn)
    }
  }
  return result
}
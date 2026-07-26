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

package org.jetbrains.plugins.grails.tests

import com.intellij.openapi.module.ModuleUtil
import com.intellij.openapi.util.text.StringUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.grails.structure.GrailsApplication

fun getTestsForArtifact(application: GrailsApplication, artefact: PsiClass, result: MutableCollection<in PsiClass>): Unit {
  val module = ModuleUtil.findModuleForPsiElement(artefact) ?: return

  val qualifiedName = artefact.qualifiedName ?: return
  val packageName = StringUtil.getPackageName(qualifiedName)
  val shortName = StringUtil.getShortName(qualifiedName)
  val unitTestFqn = StringUtil.getQualifiedName(packageName, shortName + "Spec")

  val scope = GlobalSearchScope.moduleWithDependentsScope(module)
  val clazz = JavaPsiFacade.getInstance(application.project).findClass(unitTestFqn, scope) ?: return
  result.add(clazz)
}
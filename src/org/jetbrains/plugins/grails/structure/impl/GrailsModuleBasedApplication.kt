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

package org.jetbrains.plugins.grails.structure.impl

import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider.Result
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.plugins.grails.structure.GrailsApplicationBase

abstract class GrailsModuleBasedApplication(
  val module: Module,
  root: VirtualFile
) : GrailsApplicationBase(module.project, root) {

  override val isValid: Boolean get() = super.isValid && !module.isDisposed

  override fun getScope(includeDependencies: Boolean, testsOnly: Boolean): GlobalSearchScope {
    return when {
      testsOnly && includeDependencies -> module.moduleTestsWithDependentsScope
      testsOnly && !includeDependencies -> CachedValuesManager.getManager(project).getCachedValue(this) {
        val moduleScope = module.getModuleScope(true)
        val moduleWithoutTestsScope = module.getModuleScope(false)
        val result = moduleScope.intersectWith(GlobalSearchScope.notScope(moduleWithoutTestsScope))
        Result.create(result, ProjectRootManager.getInstance(project))
      }
      !testsOnly && includeDependencies -> module.getModuleRuntimeScope(false)
      !testsOnly && !includeDependencies -> module.getModuleScope(false)
      else -> throw RuntimeException("never happens")
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false
    if (!super.equals(other)) return false

    other as GrailsModuleBasedApplication

    return module == other.module
  }

  override fun hashCode(): Int = 31 * super.hashCode() + module.hashCode()
}
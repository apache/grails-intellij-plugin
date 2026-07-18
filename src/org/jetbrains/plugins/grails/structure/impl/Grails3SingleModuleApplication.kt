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

import com.intellij.openapi.externalSystem.model.DataNode
import com.intellij.openapi.externalSystem.model.project.ModuleData
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope

internal class Grails3SingleModuleApplication(
    val module: Module,
    root: VirtualFile,
    moduleDataNode: DataNode<ModuleData>
) : Grails3ApplicationBase(module.project, root, moduleDataNode) {

  override fun getScope(includeDependencies: Boolean, testsOnly: Boolean): GlobalSearchScope {
    return if (includeDependencies) {
      module.getModuleRuntimeScope(testsOnly)
    }
    else {
      module.getModuleScope(testsOnly)
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (other?.javaClass != javaClass) return false
    if (!super.equals(other)) return false

    other as Grails3SingleModuleApplication

    return module == other.module
  }

  override fun hashCode() = 31 * super.hashCode() + module.hashCode()
}

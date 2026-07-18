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
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope.EMPTY_SCOPE
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData

internal class Grails3MultiModuleApplication(
    project: Project,
    root: VirtualFile,
    moduleDataNode: DataNode<ModuleData>
) : Grails3ApplicationBase(project, root, moduleDataNode) {

  override fun getScope(includeDependencies: Boolean, testsOnly: Boolean): GlobalSearchScope {
    fun getScope(sourceSetName: String) = findModule(sourceSetName)?.let {
      if (includeDependencies) it.getModuleRuntimeScope(testsOnly) else it.getModuleScope(testsOnly)
    } ?: EMPTY_SCOPE

    return when {
      testsOnly -> getScope("test").uniteWith(getScope("integrationTest"))
      else -> getScope("main")
    }
  }

  private val mySourceSets by lazy {
    ExternalSystemApiUtil.findAll(moduleDataNode, GradleSourceSetData.KEY).map { it.data }
  }

  private fun findModule(sourceSetName: String): Module? {
    val sourceSet = mySourceSets.find { it.id.endsWith(":$sourceSetName") } ?: return null
    return ModuleManager.getInstance(project).modules.find { ExternalSystemApiUtil.getExternalProjectId(it) == sourceSet.id }
  }
}
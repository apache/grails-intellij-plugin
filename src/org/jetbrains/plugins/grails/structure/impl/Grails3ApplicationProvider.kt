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

import com.intellij.openapi.externalSystem.service.project.ProjectDataManager
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil.findModule
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings
import org.jetbrains.plugins.gradle.util.GradleConstants.SYSTEM_ID
import org.jetbrains.plugins.grails.gradle.GrailsModuleData
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.structure.GrailsApplicationProvider

private val LOG = com.intellij.openapi.diagnostic.logger<Grails3ApplicationProvider>()

class Grails3ApplicationProvider : GrailsApplicationProvider() {

  override fun createApplication(project: Project, root: VirtualFile): GrailsApplication? {
    val path = root.path

    val settings = ExternalSystemApiUtil.getSettings(project, SYSTEM_ID)
    val linkedProjectSettings = settings.getLinkedProjectSettings(path) as? GradleProjectSettings ?: run {
      LOG.warn("GRAILS-DBG createApplication: no linkedProjectSettings for path=$path; linked=${settings.linkedProjectsSettings.map { it.externalProjectPath }}")
      return null
    }
    val gradleProjectInfo = ProjectDataManager.getInstance().getExternalProjectData(
        project, SYSTEM_ID, linkedProjectSettings.externalProjectPath
    ) ?: run {
      LOG.warn("GRAILS-DBG createApplication: no externalProjectData for ${linkedProjectSettings.externalProjectPath}")
      return null
    }

    val moduleData = findModule(gradleProjectInfo.externalProjectStructure, path) ?: run {
      LOG.warn("GRAILS-DBG createApplication: findModule null for path=$path")
      return null
    }
    if (ExternalSystemApiUtil.find(moduleData, GrailsModuleData.KEY) == null) {
      LOG.warn("GRAILS-DBG createApplication: no GrailsModuleData for path=$path")
      return null
    }
    LOG.warn("GRAILS-DBG createApplication: SUCCESS for path=$path")

    return if (linkedProjectSettings.isResolveModulePerSourceSet) {
      Grails3MultiModuleApplication(project, root, moduleData)
    }
    else {
      ProjectRootManager.getInstance(project).fileIndex.getModuleForFile(root)?.let { module ->
        Grails3SingleModuleApplication(module, root, moduleData)
      }
    }
  }
}

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
package com.intellij.groovy.grails.maven

import com.intellij.facet.FacetManager
import com.intellij.facet.ModifiableFacetModel
import com.intellij.openapi.application.ApplicationManager
import com.intellij.util.Consumer
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator
import org.jetbrains.idea.maven.project.MavenProject
import org.jetbrains.plugins.grails.config.GrailsFramework
import org.jetbrains.plugins.grails.util.GrailsFacetProvider
import java.util.stream.Stream

/**
 * Replaces the legacy [org.jetbrains.idea.maven.importing.MavenImporter]-based
 * GrailsMavenImporter, which is no longer invoked by the workspace-model Maven import
 * (2026.2): registers the Grails source/test/resource roots and the Grails facets for
 * projects built with org.grails:grails-maven-plugin.
 */
internal class GrailsMavenWorkspaceConfigurator : MavenWorkspaceConfigurator {

  private fun isApplicable(mavenProject: MavenProject): Boolean =
    mavenProject.findPlugin("org.grails", "grails-maven-plugin") != null

  override fun getAdditionalFolders(
    context: MavenWorkspaceConfigurator.FoldersContext,
  ): Stream<MavenWorkspaceConfigurator.AdditionalFolder> {
    if (!isApplicable(context.mavenProject)) {
      return Stream.empty()
    }
    val folders = ArrayList<MavenWorkspaceConfigurator.AdditionalFolder>()
    for (rootType in GrailsFramework.GRAILS_SOURCE_FOLDERS.keySet()) {
      val folderType = rootType.toFolderType()
      GrailsFramework.GRAILS_SOURCE_FOLDERS.get(rootType)
        .mapTo(folders) { MavenWorkspaceConfigurator.AdditionalFolder(it, folderType) }
    }
    return folders.stream()
  }

  override fun afterModelApplied(context: MavenWorkspaceConfigurator.AppliedModelContext) {
    for (projectWithModules in context.mavenProjectsWithModules) {
      val mavenProject = projectWithModules.mavenProject
      if (!isApplicable(mavenProject)) continue

      for (moduleWithType in projectWithModules.modules) {
        val module = moduleWithType.module
        val actions = ArrayList<Consumer<ModifiableFacetModel>>()
        for (provider in GrailsFacetProvider.EP_NAME.extensions) {
          provider.addFacets(actions, module, listOf(mavenProject.directoryFile))
        }
        if (actions.isEmpty()) continue

        ApplicationManager.getApplication().invokeAndWait {
          ApplicationManager.getApplication().runWriteAction {
            if (module.isDisposed) return@runWriteAction
            val facetModel = FacetManager.getInstance(module).createModifiableModel()
            actions.forEach { it.consume(facetModel) }
            facetModel.commit()
          }
        }
      }
    }
  }
}

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

package org.jetbrains.plugins.grails.projectView.impl

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiFileSystemItem
import org.jetbrains.plugins.grails.projectView.GrailsPluginsNode
import org.jetbrains.plugins.grails.projectView.NodeWeights.SRC_FOLDERS
import org.jetbrains.plugins.grails.projectView.api.GrailsViewNodeProvider
import org.jetbrains.plugins.grails.projectView.nodes.GrailsPsiDirectoryNode
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.util.version.Version.GRAILS_3_0

class Grails3NodeProvider : GrailsViewNodeProvider {

  private val specialFiles = listOf("build.gradle", "settings.gradle", "gradle.properties")
  private val specialDirs = listOf("src/main/scripts", "src/main/webapp")

  override fun createNodes(application: GrailsApplication, settings: ViewSettings): Collection<AbstractTreeNode<*>> {
    if (application.grailsVersion >= GRAILS_3_0) {
      val result = mutableListOf<AbstractTreeNode<*>>()

      val specialDirs = specialDirs.mapNotNull { application.findPsiDirectory(it) }

      result += specialDirs.map {
        GrailsPsiDirectoryNode(it, settings, nodeWeight = SRC_FOLDERS)
      }

      application.findPsiDirectory("src")?.let {
        val filter: (PsiFileSystemItem) -> Boolean = { it !in specialDirs && shouldShowItem(it) }
        result += GrailsPsiDirectoryNode(it, settings, nodeWeight = SRC_FOLDERS, filter = filter)
      }

      result += specialFiles.mapNotNull { application.findPsiFile(it) }.map {
        PsiFileNode(application.project, it, settings)
      }

      result += GrailsPluginsNode(application.project, settings)
      return result
    }
    return emptyList()
  }
}
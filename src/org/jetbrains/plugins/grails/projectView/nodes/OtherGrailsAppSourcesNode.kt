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

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.ide.projectView.PresentationData
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiManager
import com.intellij.util.PlatformIcons
import com.intellij.util.lazyUnsafe
import org.jetbrains.plugins.grails.artefact.api.displayableArtefactHandlers
import org.jetbrains.plugins.grails.projectView.NodeWeights
import org.jetbrains.plugins.grails.projectView.impl.shouldShowItem
import org.jetbrains.plugins.grails.projectView.impl.specialGrailsAppFolders
import org.jetbrains.plugins.grails.structure.GrailsApplication

class OtherGrailsAppSourcesNode(
  directory: PsiDirectory,
  settings: ViewSettings
) : GrailsPsiDirectoryNode(
  directory,
  settings,
  nodeWeight = NodeWeights.OTHER_GRAILS_APP_FOLDER,
  nodeIcon = PlatformIcons.SOURCE_FOLDERS_ICON
) {

  val grailsApplication: GrailsApplication by lazyUnsafe { findNotNullValueOfType<GrailsApplication>() }

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {
    val application = grailsApplication
    val project = application.project
    val manager = PsiManager.getInstance(project)

    // directories from nodes that are showed separately
    val artefactDirs = displayableArtefactHandlers.filter {
      it.isVisible(application)
    }.mapNotNullTo(HashSet()) {
      it.getDirectory(application)
    }.filter {
      it.name !in specialGrailsAppFolders.keys
    }

    // other grails-app directories excluding special, special are showed separately
    val otherDirs = application.appRoot.children.filter {
      it !in artefactDirs && it.name !in specialGrailsAppFolders.keys
    }

    // do not show artefact nodes under these directories
    val artefactDirNodes = artefactDirs.mapNotNull {
      manager.findDirectory(it)
    }.map { PsiDirectoryNode(project, it, settings, ::shouldShowItem) }

    val otherDirNodes = otherDirs.mapNotNull {
      manager.findDirectory(it)
    }.map { PsiDirectoryNode(project, it, settings) }

    return artefactDirNodes + otherDirNodes
  }

  override fun contains(file: VirtualFile): Boolean = super.contains(file) && PsiManager.getInstance(project!!).findFile(file)?.let {
    shouldShowItem(it)
  } ?: false

  override fun updateImpl(data: PresentationData) {
    super.updateImpl(data)
    data.apply {
      locationString = "Other sources"
    }
  }
}
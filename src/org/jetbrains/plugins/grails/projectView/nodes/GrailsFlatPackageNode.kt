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
import com.intellij.ide.util.treeView.TreeViewUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.ui.IconManager
import com.intellij.ui.PlatformIcons

internal class GrailsFlatPackageNode(
  project: Project,
  settings: ViewSettings,
  override val packageFqn: String
) : GrailsArtefactPackageNode<GrailsPackageValue>(project, settings, GrailsPackageValue(packageFqn)) {
  override fun update(presentation: PresentationData) {
    presentation.setIcon(IconManager.getInstance().getPlatformIcon(PlatformIcons.Package))
    presentation.presentableText = if (settings.isAbbreviatePackageNames) {
      JavaPsiFacade.getInstance(project!!).findPackage(packageFqn)
        ?.let(TreeViewUtil::calcAbbreviatedPackageFQName)
      ?: packageFqn
    }
    else {
      packageFqn
    }
  }

  override fun getChildren(): TreeNodes = artefacts.getClassNodes(project!!, settings, artefactHandler, packageFqn)

  override fun contains(file: VirtualFile): Boolean = !file.isDirectory && file.parent in nodeDirectories

  override val nodeDirectories: Collection<VirtualFile>
    get() = packageDirectories(packageFqn)
}

internal data class GrailsPackageValue(val packageFqn: String)
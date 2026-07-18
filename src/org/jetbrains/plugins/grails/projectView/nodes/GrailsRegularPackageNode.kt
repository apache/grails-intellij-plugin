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
import com.intellij.ide.util.treeView.ValidateableNode
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.IconManager
import com.intellij.ui.PlatformIcons

class GrailsRegularPackageNode(
  project: Project,
  settings: ViewSettings,
  fqns: CompactedFqn
) : GrailsArtefactPackageNode<CompactedFqn>(project, settings, fqns), ValidateableNode {

  override val packageFqn get() = value.toString()

  /**
   * This method checks if this node contains compacted fqns and that all compacted fqns are still empty.
   *
   * Consider the structure:
   * ```
   * - com
   *   - foo.bar // <- this node
   *     - Something
   * ```
   * In the above case we need to ensure that `com.foo` package is still empty.
   * If there is something to show under `com.foo`, then we consider this node invalid,
   * so `com` children would be recomputed, resulting in the new structure:
   * ```
   * - com
   *     - foo
   *       - SomethingNew
   *       - bar
   *         - Something
   * ```
   */
  override fun isValid(): Boolean {
    val value = value
    if (value.hasCompactedFqns && settings.isHideEmptyMiddlePackages) {
      val (baseFqn, relativeParts) = value
      val newPackages = getPackagesRegular(artefacts, baseFqn, true)
      return relativeParts in newPackages
    }
    return true
  }

  override fun update(presentation: PresentationData) {
    presentation.setIcon(IconManager.getInstance().getPlatformIcon(PlatformIcons.Package))
    presentation.presentableText = value.relativeParts.fqnString()
  }

  override fun getChildren(): TreeNodes = getNodesRegular(artefacts, project!!, settings, artefactHandler, value.allParts)

  override val nodeDirectories: Collection<VirtualFile> get() = value.expandedFqns.flatMapTo(HashSet(), ::packageDirectories)

  override fun contains(file: VirtualFile): Boolean {
    return packageDirectories(value.expandedFqns.first()).any {
      VfsUtil.isAncestor(it, file, true)
    }
  }
}

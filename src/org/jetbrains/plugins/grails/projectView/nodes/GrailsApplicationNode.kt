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
import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.SimpleTextAttributes
import org.jetbrains.plugins.grails.GrailsBundle
import org.jetbrains.plugins.grails.projectView.api.EP_NAME
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager

class GrailsApplicationNode(
  application: GrailsApplication,
  viewSettings: ViewSettings
) : ProjectViewNode<GrailsApplication>(application.project, application, viewSettings) {

  override fun shouldUpdateData(): Boolean {
    return value.isValid && super.shouldUpdateData()
  }

  override fun getChildren(): Collection<AbstractTreeNode<*>> = EP_NAME.extensions.flatMap {
    it.createNodes(value, settings)
  }

  override fun contains(file: VirtualFile): Boolean {
    return project?.let { GrailsApplicationManager.getInstance(it).findApplication(file) == value } ?: false
  }

  override fun update(presentation: PresentationData) {
    presentation.apply {
      val application = value
      setIcon(application.icon)
      addText(application.name, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
      application.appVersion?.let { version ->
        addText(" ${version}", SimpleTextAttributes.REGULAR_ATTRIBUTES) // NON-NLS
      }
      addText(" " + GrailsBundle.message("project.view.application.node.version.label", application.grailsVersion), SimpleTextAttributes.REGULAR_ATTRIBUTES)
      tooltip = application.root.path
    }
  }
}

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

package org.jetbrains.plugins.grails.projectView

import com.intellij.ide.impl.ProjectViewSelectInTarget
import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.AbstractProjectViewPaneWithAsyncSupport
import com.intellij.ide.projectView.impl.ProjectTreeStructure
import com.intellij.ide.projectView.impl.ProjectViewTree
import com.intellij.ide.util.treeView.NodeDescriptor
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.config.GrailsConstants
import org.jetbrains.plugins.grails.projectView.impl.GrailsNodeComparator
import org.jetbrains.plugins.grails.projectView.nodes.GrailsRootNode
import javax.swing.Icon
import javax.swing.tree.DefaultTreeModel

internal class GrailsProjectViewPane(val project: Project) : AbstractProjectViewPaneWithAsyncSupport(project) {
  override fun getId(): String = GrailsConstants.GRAILS

  override fun getIcon(): Icon = GroovyMvcIcons.Grails

  override fun getTitle(): String = id

  override fun getWeight(): Int = 13

  override fun createSelectInTarget(): ProjectViewSelectInTarget = object : ProjectViewSelectInTarget(project), DumbAware {

    override fun toString() = title

    override fun getMinorViewId() = id

    override fun getWeight() = 5.239f
  }

  override fun createStructure(): ProjectTreeStructure = object : ProjectTreeStructure(project, id) {

    override fun createRoot(project: Project, settings: ViewSettings) = GrailsRootNode(project, settings)

    override fun isToBuildChildrenInBackground(element: Any) = true
  }

  override fun createTree(treeModel: DefaultTreeModel): ProjectViewTree = ProjectViewTree(treeModel)

  override fun createComparator(): Comparator<NodeDescriptor<*>> = GrailsNodeComparator(project, id)

  override fun isInitiallyVisible(): Boolean = false
}

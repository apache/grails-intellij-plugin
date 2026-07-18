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
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager

class GrailsRootNode(project: Project, viewSettings: ViewSettings) : ProjectViewNode<Project>(project, project, viewSettings) {

  override fun getChildren(): List<GrailsApplicationNode> = GrailsApplicationManager.getInstance(value).applications.map {
    GrailsApplicationNode(it, settings)
  }

  override fun update(presentation: PresentationData): Unit = Unit

  override fun contains(file: VirtualFile): Boolean = GrailsApplicationManager.getInstance(value).findApplication(file) != null
}

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
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler

class GrailsArtefactHandlerNode(
  project: Project,
  artefactHandler: GrailsDisplayableArtefactHandler,
  viewSettings: ViewSettings
) : GrailsArtefactHandlerNodeBase<GrailsDisplayableArtefactHandler>(project, viewSettings, artefactHandler) {

  override val artefactHandler: GrailsDisplayableArtefactHandler get() = value

  override fun update(presentation: PresentationData) {
    presentation.setIcon(value.groupIcon)
    presentation.presentableText = value.title
  }

  override fun getChildren(): TreeNodes = getArtefactNodes(project!!, settings, value, artefacts)

  override fun contains(file: VirtualFile): Boolean = file.isDirectory || mayContain(grailsApplication, file)
}

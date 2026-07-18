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

package org.jetbrains.plugins.grails.artefact.impl

import com.intellij.icons.AllIcons
import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiClass
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler
import org.jetbrains.plugins.grails.projectView.NodeWeights
import org.jetbrains.plugins.grails.projectView.nodes.GrailsControllerNode
import org.jetbrains.plugins.grails.structure.GrailsApplication
import javax.swing.Icon

object ControllerArtefactHandler : GrailsDisplayableArtefactHandler {

  override val artefactHandlerID: String = "Controller"

  override fun getDirectory(application: GrailsApplication): VirtualFile? = application.appRoot.findChild("controllers")

  override val annotationFqns: Collection<String> get() = listOf("grails.web.Controller")

  override val icon: Icon = AllIcons.Nodes.Controller

  override val groupIcon: Icon get() = AllIcons.Nodes.KeymapTools

  override val title: String = "Controllers"

  override val weight: Int = NodeWeights.CONTROLLERS_FOLDER

  override fun createNode(artefact: PsiClass, settings: ViewSettings): GrailsControllerNode = GrailsControllerNode(artefact, settings)
}
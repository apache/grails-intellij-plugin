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
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler
import org.jetbrains.plugins.grails.projectView.NodeWeights
import org.jetbrains.plugins.grails.structure.GrailsApplication
import javax.swing.Icon

object DomainArtefactHandler : GrailsDisplayableArtefactHandler {

  override val artefactHandlerID: String = "Domain"

  override val artefactClassSuffix: String = ""

  override val annotationFqns: Collection<String> = listOf("grails.persistence.Entity", "grails.gorm.annotation.Entity")

  override fun getDirectory(application: GrailsApplication): VirtualFile? = application.appRoot.findChild("domain")

  override val icon: Icon = AllIcons.Nodes.DataTables

  override val groupIcon: Icon = AllIcons.Nodes.Models

  override val title: String = "Domain Classes"

  override val weight: Int = NodeWeights.DOMAIN_CLASSES_FOLDER
}
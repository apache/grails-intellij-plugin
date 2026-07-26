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

import com.intellij.ide.projectView.ProjectViewNode
import com.intellij.ide.projectView.ViewSettings
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.GrailsArtefacts
import org.jetbrains.plugins.grails.structure.GrailsApplication

abstract class GrailsArtefactHandlerNodeBase<T : Any>(
  project: Project,
  viewSettings: ViewSettings,
  value: T
) : ProjectViewNode<T>(project, value, viewSettings) {

  val grailsApplication: GrailsApplication get() = findNotNullValueOfType()
  protected val scope: GlobalSearchScope get() = grailsApplication.getScope(includeDependencies = false, testsOnly = false)
  protected abstract val artefactHandler: GrailsDisplayableArtefactHandler
  protected val artefacts: Classes get() = GrailsArtefacts.getArtefacts(artefactHandler, grailsApplication, scope)
}

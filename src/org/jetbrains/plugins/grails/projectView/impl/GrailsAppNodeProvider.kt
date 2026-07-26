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

package org.jetbrains.plugins.grails.projectView.impl

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.grails.artefact.api.ArtefactHandlers
import org.jetbrains.plugins.grails.projectView.api.GrailsViewNodeProvider
import org.jetbrains.plugins.grails.projectView.nodes.GrailsArtefactHandlerNode
import org.jetbrains.plugins.grails.projectView.nodes.GrailsPsiDirectoryNode
import org.jetbrains.plugins.grails.projectView.nodes.OtherGrailsAppSourcesNode
import org.jetbrains.plugins.grails.structure.GrailsApplication

internal class GrailsAppNodeProvider : GrailsViewNodeProvider {
  override fun createNodes(application: GrailsApplication, settings: ViewSettings): Collection<AbstractTreeNode<*>> {
    val project = application.project

    val result = mutableListOf<AbstractTreeNode<*>>()

    ArtefactHandlers.displayableArtefactHandlers().filter { it.isVisible(application) }.mapTo(result) {
      GrailsArtefactHandlerNode(project, it, settings)
    }

    for ((name, data) in specialGrailsAppFolders) {
      application.findAppPsiDirectory(name)?.let {
        result += GrailsPsiDirectoryNode(it, settings, data.first, data.second, data.third, ::shouldShowItem)
      }
    }

    PsiManager.getInstance(project).findDirectory(application.appRoot)?.let {
      result += OtherGrailsAppSourcesNode(it, settings)
    }

    return result
  }
}
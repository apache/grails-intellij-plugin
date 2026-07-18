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

package com.intellij.groovy.grails.maven

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiManager
import org.jetbrains.idea.maven.model.MavenConstants
import org.jetbrains.plugins.grails.projectView.api.GrailsSingleNodeProvider
import org.jetbrains.plugins.grails.structure.GrailsApplication

internal class MavenNodeProvider : GrailsSingleNodeProvider() {

  override fun createNode(application: GrailsApplication, settings: ViewSettings): AbstractTreeNode<*>? {
    if (application !is GrailsMavenApplication) return null
    val project = application.project
    return application.root.findChild(MavenConstants.POM_XML)
      ?.let { PsiManager.getInstance(project).findFile(it) }
      ?.let { PsiFileNode(project, it, settings) }
  }
}
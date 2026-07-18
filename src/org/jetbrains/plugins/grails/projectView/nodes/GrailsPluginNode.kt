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
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.ui.SimpleTextAttributes
import com.intellij.util.lazyUnsafe
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.plugins.GrailsPluginDescriptor
import org.jetbrains.plugins.grails.structure.GrailsApplication

class GrailsPluginNode(
    project: Project,
    value: GrailsPluginDescriptor,
    settings: ViewSettings
) : AbstractPsiBasedNode<GrailsPluginDescriptor>(project, value, settings) {

  init {
    myName = value.pluginName
  }

  val grailsApplication: GrailsApplication by lazyUnsafe { findNotNullValueOfType<GrailsApplication>() }

  override fun extractPsiFromValue(): PsiClass? = value?.pluginClass

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> = emptyList()

  override fun isAlwaysLeaf(): Boolean = true

  override fun updateImpl(data: PresentationData) {
    data.apply {
      setIcon(GroovyMvcIcons.Groovy_mvc_plugin)
      addText(value.pluginName, SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES)
      addText(" ${value.pluginVersion ?: grailsApplication.grailsVersion}", SimpleTextAttributes.REGULAR_ATTRIBUTES)
    }
  }
}
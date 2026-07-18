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

import com.intellij.ide.projectView.ViewSettings
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiClass
import com.intellij.util.lazyUnsafe
import org.jetbrains.plugins.grails.artefact.impl.controllers.getActions
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition

class GrailsControllerNode(clazz: PsiClass, settings: ViewSettings) : ClassTreeNode(clazz.project, clazz, settings) {

  val grailsApplication: GrailsApplication by lazyUnsafe { findNotNullValueOfType<GrailsApplication>() }

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>>? {
    if (!settings.isShowMembers) return null
    val clazz = value as? GrTypeDefinition ?: return null
    return getActions(clazz, grailsApplication).map {
      GrailsActionNode(it.key, it.value, settings)
    }
  }
}
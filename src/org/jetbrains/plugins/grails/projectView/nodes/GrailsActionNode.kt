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
import com.intellij.ide.projectView.impl.nodes.BasePsiMemberNode
import com.intellij.ide.projectView.impl.nodes.PsiFileNode
import com.intellij.ide.util.treeView.AbstractTreeNode
import com.intellij.psi.PsiMember
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.util.GrailsUtils

class GrailsActionNode(
  val actionName: String,
  action: PsiMember,
  settings: ViewSettings
) : BasePsiMemberNode<PsiMember>(action.project, action, settings) {

  override fun getChildrenImpl(): Collection<AbstractTreeNode<*>> {
    return GrailsUtils.getViewPsiByAction(value).map { PsiFileNode(project, it, settings) }
  }

  override fun updateImpl(data: PresentationData) {
    data.apply {
      presentableText = actionName
      setIcon(GroovyMvcIcons.Action_method)
    }
  }
}
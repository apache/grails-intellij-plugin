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

package org.jetbrains.plugins.grails.editor.toolbar

import com.intellij.ide.presentation.VirtualFilePresentation
import com.intellij.ide.util.PsiNavigationSupport
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.actions.ArtefactData
import javax.swing.Icon

abstract class GrailsToolbarVfileAction : GrailsToolbarTargetAction<VirtualFile>() {

  @ActionText override fun getNavigateTitle(target: VirtualFile): String = target.name

  override fun getNavigateIcon(target: VirtualFile): Icon? = VirtualFilePresentation.getIcon(target)

  override fun navigate(artefactData: ArtefactData, target: VirtualFile): Unit = PsiNavigationSupport.getInstance().createNavigatable(artefactData.project, target, -1).navigate(true)
}

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

import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.util.NlsActions.ActionText
import org.jetbrains.plugins.grails.actions.ArtefactData
import javax.swing.Icon

/**
 * This class constructs navigate actions from targets.
 * It does not care about type of navigatable because actual navigate logic is delegated to inheritors.
 *
 * @param T type of target, e.g. PsiClass or VirtualFile.
 */
abstract class GrailsToolbarTargetAction<T> : GrailsToolbarActionBase() {

  override fun createNavigateActions(artefactData: ArtefactData): Collection<AnAction> = tryGetNavigateTargets(artefactData).map {
    object : AnAction(getNavigateTitle(it), null, getNavigateIcon(it)) {
      override fun actionPerformed(e: AnActionEvent) = navigate(artefactData, it)
    }
  }

  abstract fun getNavigateTargets(artefactData: ArtefactData): Collection<T>

  @ActionText abstract fun getNavigateTitle(target: T): String?

  abstract fun getNavigateIcon(target: T): Icon?

  abstract fun navigate(artefactData: ArtefactData, target: T): Unit

  private fun tryGetNavigateTargets(artefactData: ArtefactData): Collection<T> =
      if (DumbService.isDumb(artefactData.project)) emptyList()
      else getNavigateTargets(artefactData)
}
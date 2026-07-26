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

package org.jetbrains.plugins.grails.editor

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.actionSystem.ToggleAction
import com.intellij.ui.EditorNotifications

class ViewGrailsEditorToolbarAction : ToggleAction() {

  override fun update(e: AnActionEvent) {
    super.update(e)
    e.presentation.isVisible = isVisible(e)
  }

  private fun isVisible(e: AnActionEvent): Boolean {
    val project = e.project ?: return false
    val fileEditor = e.getData(PlatformCoreDataKeys.FILE_EDITOR) ?: return false
    val file = fileEditor.file ?: return false
    return shouldBeDecorated(file, fileEditor, project)
  }

  override fun getActionUpdateThread(): ActionUpdateThread {
    return ActionUpdateThread.BGT
  }

  override fun isSelected(e: AnActionEvent): Boolean {
    return showEditorToolBar
  }

  override fun setSelected(e: AnActionEvent, state: Boolean) {
    showEditorToolBar = state
    val project = e.project ?: return
    EditorNotifications.getInstance(project).updateAllNotifications()
  }
}
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
package org.apache.grails.intellij.plugin.editor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotifications;
import org.jetbrains.annotations.NotNull;

public class ViewGrailsEditorToolbarAction extends ToggleAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    super.update(e);
    e.getPresentation().setVisible(isVisible(e));
  }

  private static boolean isVisible(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return false;
    FileEditor fileEditor = e.getData(PlatformCoreDataKeys.FILE_EDITOR);
    if (fileEditor == null) return false;
    VirtualFile file = fileEditor.getFile();
    if (file == null) return false;
    return GrailsEditorToolbar.shouldBeDecorated(file, fileEditor, project);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public boolean isSelected(@NotNull AnActionEvent e) {
    return GrailsEditorToolbar.isShowEditorToolbar();
  }

  @Override
  public void setSelected(@NotNull AnActionEvent e, boolean state) {
    GrailsEditorToolbar.setShowEditorToolbar(state);
    Project project = e.getProject();
    if (project == null) return;
    EditorNotifications.getInstance(project).updateAllNotifications();
  }
}

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
package org.jetbrains.plugins.grails.editor;

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import java.util.function.Function;

public final class GrailsEditorDecorator implements EditorNotificationProvider {

  @Override
  public @Nullable Function<? super FileEditor, ? extends JComponent> collectNotificationData(@NotNull Project project,
                                                                                             @NotNull VirtualFile file) {
    if (!GrailsEditorToolbar.isShowEditorToolbar()) return null;
    if (!GrailsEditorToolbar.shouldBeDecorated(project, file)) {
      return null;
    }
    return GrailsEditorDecorator::createNotificationPanel;
  }

  private static @Nullable JComponent createNotificationPanel(@NotNull FileEditor fileEditor) {
    if (!GrailsEditorToolbar.shouldBeDecorated(fileEditor)) {
      return null;
    }
    ActionManager manager = ActionManager.getInstance();
    ActionGroup group = (ActionGroup)manager.getAction("grails.toolbar");
    ActionToolbar toolbar = manager.createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, group, true);
    toolbar.setTargetComponent(fileEditor.getComponent());
    return toolbar.getComponent();
  }
}

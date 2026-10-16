/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.grails.intellij.plugin.actions;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import org.apache.grails.intellij.plugin.config.GrailsConstants;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersConfigurable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ConfigureCustomFoldersAction extends DumbAwareAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    e.getPresentation().setEnabledAndVisible(
      project != null && offersSettings(isGrailsPane(project), isCustomFolderSelected(e), selectedDirectory(e) != null));
  }

  /**
   * The visibility rule. The pane and the selection arrive as separate inputs because a light
   * fixture has no Grails pane to make current, so the rule is asserted on its own rather than
   * through a pane that cannot exist.
   */
  static boolean offersSettings(boolean inGrailsPane, boolean customFolderSelected, boolean directorySelected) {
    return inGrailsPane && (customFolderSelected || directorySelected);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    ShowSettingsUtil.getInstance().showSettingsDialog(project, GrailsCustomFoldersConfigurable.class);
  }

  /**
   * The only pane lookup the custom-folder actions still make: the settings are about the Grails
   * pane, so it is the only pane they are offered in.
   */
  static boolean isGrailsPane(@NotNull Project project) {
    return isGrailsPane(ProjectView.getInstance(project).getCurrentProjectViewPane());
  }

  static boolean isGrailsPane(@Nullable AbstractProjectViewPane pane) {
    return pane != null && GrailsConstants.GRAILS.equals(pane.getId());
  }

  static boolean isCustomFolderSelected(@NotNull AnActionEvent e) {
    return CustomFolderSelection.customFolder(e) != null;
  }

  static @Nullable PsiDirectory selectedDirectory(@NotNull AnActionEvent e) {
    return CustomFolderSelection.directory(e);
  }
}

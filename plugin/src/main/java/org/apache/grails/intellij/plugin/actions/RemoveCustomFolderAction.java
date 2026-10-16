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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import org.apache.grails.intellij.plugin.config.FolderPaths;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersProjectService;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsCustomFolder;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class RemoveCustomFolderAction extends DumbAwareAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    boolean visible = false;
    if (project != null && GrailsApplicationManager.getInstance(project).hasApplications()) {
      GrailsCustomFolder folder = selectedCustomFolder(e);
      visible = folder != null && isProjectFolder(project, folder.getConfiguredPath());
    }
    e.getPresentation().setEnabledAndVisible(visible);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) return;
    GrailsCustomFolder folder = selectedCustomFolder(e);
    if (folder == null) return;

    GrailsCustomFoldersProjectService service = project.getService(GrailsCustomFoldersProjectService.class);
    List<String> folders = new ArrayList<>(service.getFolders());
    // Folders may have changed between update and actionPerformed (e.g. another settings dialog
    // closed); if the selected folder is no longer in the project's list, there is nothing to do.
    if (!folders.remove(folder.getConfiguredPath())) return;
    service.setFolders(folders);
  }

  /** The selection itself is read by {@link CustomFolderSelection#customFolder(AnActionEvent)}. */
  static @Nullable GrailsCustomFolder selectedCustomFolder(@NotNull AnActionEvent e) {
    return CustomFolderSelection.customFolder(e);
  }

  static boolean isProjectFolder(@NotNull Project project, @NotNull String configuredPath) {
    String normalized = FolderPaths.normalize(configuredPath);
    GrailsCustomFoldersProjectService service = project.getService(GrailsCustomFoldersProjectService.class);
    for (String folder : service.getFolders()) {
      if (FolderPaths.normalize(folder).equals(normalized)) return true;
    }
    return false;
  }
}

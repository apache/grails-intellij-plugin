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
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.config.FolderPaths;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersAppService;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersProjectService;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class AddCustomFolderAction extends DumbAwareAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    Project project = e.getProject();
    if (project == null) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }
    PsiDirectory directory = CustomFolderSelection.directory(e);
    GrailsApplication application = applicationOf(project, directory);
    if (directory == null || application == null || isApplicationRoot(application, directory)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    GrailsCustomFoldersProjectService service = project.getService(GrailsCustomFoldersProjectService.class);
    String path = relativeToApplicationRoot(application, directory);
    // Both no-ops: a folder the Grails view already renders is the one right-clicked in the tree, and
    // a directory outside the application root has no path to store against that base. Hidden rather
    // than disabled, because neither is a state the user can act their way out of.
    if (path == null || isAlreadyConfigured(service, path)) {
      e.getPresentation().setEnabledAndVisible(false);
      return;
    }

    // The cap counts what the Grails view will show, which is this project's folders plus the global
    // defaults, so a project with one folder and five global ones is already at six.
    int max = GrailsCustomFoldersAppService.getStaticInstance().getMaxFolders();
    if (service.getEffectiveFolders().size() >= max) {
      e.getPresentation().setVisible(true);
      e.getPresentation().setEnabled(false);
      e.getPresentation().setDescription(
          GrailsBundle.message("grails.settings.custom.folders.cap.reached", max));
    }
    else {
      e.getPresentation().setEnabledAndVisible(true);
    }
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    // No read action is taken here, so nothing here may touch PSI or the VFS on its own; the one
    // lookup this path needs lives in CustomFolderSelection and is already wrapped.
    Project project = e.getProject();
    PsiDirectory directory = CustomFolderSelection.directory(e);
    if (project == null || directory == null) return;
    GrailsApplication application = applicationOf(project, directory);
    if (application == null || isApplicationRoot(application, directory)) return;

    String path = relativeToApplicationRoot(application, directory);
    if (path == null) return;

    GrailsCustomFoldersProjectService service = project.getService(GrailsCustomFoldersProjectService.class);
    List<String> folders = new ArrayList<>(service.getFolders());
    // The same guard update() hides the action behind, so a user-mapped shortcut cannot store what
    // the context menu refuses to offer. Comparing the effective folders rather than this project's
    // own list is what keeps a global default from being promoted into a project override.
    if (!isAlreadyConfigured(service, path)) {
      folders.add(path);
      service.setFolders(folders);
    }
  }

  /**
   * Looked up under a read action because {@code findApplication} walks the content roots, which is
   * model access, and {@code update()} runs on BGT with no read action of its own. {@code update()}
   * and {@code actionPerformed()} share it so the two always agree on which application a directory
   * belongs to.
   */
  private static @Nullable GrailsApplication applicationOf(@NotNull Project project, @Nullable PsiDirectory directory) {
    if (directory == null) return null;
    VirtualFile file = directory.getVirtualFile();
    return ReadAction.computeBlocking(
      () -> GrailsApplicationManager.getInstance(project).findApplication(file));
  }

  /**
   * The path this action would store for the directory, or null when the directory does not sit under
   * the application root. The provider resolves a relative path against the application root, so that
   * is the only base that round-trips; an absolute path stored here would look for
   * {@code <root>/<absolute>} there.
   */
  private static @Nullable String relativeToApplicationRoot(@NotNull GrailsApplication application,
                                                            @NotNull PsiDirectory directory) {
    return VfsUtilCore.getRelativePath(directory.getVirtualFile(), application.getRoot(), '/');
  }

  /**
   * Whether the path is already one of the folders the Grails view renders — this project's own or a
   * global default, which the pane shows too. Compared after {@link FolderPaths#normalize} because a
   * configured {@code docs/} and a selected {@code docs} are the same folder to the provider, even
   * though they are different strings.
   */
  private static boolean isAlreadyConfigured(@NotNull GrailsCustomFoldersProjectService service,
                                             @NotNull String path) {
    String normalized = FolderPaths.normalize(path);
    for (String folder : service.getEffectiveFolders()) {
      if (FolderPaths.normalize(folder).equals(normalized)) return true;
    }
    return false;
  }

  /**
   * The application root is already the top of the Grails view — adding it as a custom folder would
   * duplicate the whole application under the application node, so the action stays hidden for it
   * in every pane.
   */
  private static boolean isApplicationRoot(@NotNull GrailsApplication application, @NotNull PsiDirectory directory) {
    return application.getRoot().equals(directory.getVirtualFile());
  }
}

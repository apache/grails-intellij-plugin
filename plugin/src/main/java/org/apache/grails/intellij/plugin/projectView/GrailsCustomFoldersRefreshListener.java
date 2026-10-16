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

package org.apache.grails.intellij.plugin.projectView;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.BulkFileListener;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.util.messages.MessageBusConnection;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.apache.grails.intellij.plugin.config.FolderPaths;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersAppService;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersProjectService;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Refreshes the Grails pane when the custom folders change. This project's folders publish on the
 * project bus and the global defaults publish on the application bus; both refresh this project, and
 * because every open project registers its own application-bus subscription a global change reaches
 * every open Grails pane exactly once. The alternative — one handler fanning out over
 * {@code ProjectManager}'s open-project list — does N work per subscriber, so N projects open means N²
 * tree rebuilds for one Apply. The third subscription watches the VFS for a configured folder
 * appearing on or disappearing from disk, which is what makes a folder that did not exist when it was
 * configured turn up when a build later creates it.
 * <p>
 * A peer class of {@code GrailsApplicationManagerRefresher} rather than a subscription inside it
 * (or in {@code GrailsStartupActivity}), so the subscription body can be driven directly by a test;
 * the {@code postStartupActivity} registration itself is what only a real IDE exercises. Hence no
 * {@code isUnitTestMode()} guard, which would only hide the wiring from the suite.
 * <p>
 * The three subscriptions coalesce through one flag held per project: the settings dialog writes the
 * global defaults and this project's own folders, so both buses fire for one Apply. Without the flag
 * that is two {@code invokeLater}s and two whole-tree rebuilds for one click. Events that arrive while
 * a rebuild is already queued are absorbed into it; events separated by real work are not, because the
 * flag is cleared as the queued rebuild starts.
 * <p>
 * The VFS subscription above uses the same flag, so a build that creates build/ and writes a
 * hundred files under it still costs one rebuild.
 * <p>
 * The flag is read off the project rather than captured per {@link #execute} because
 * {@code postStartupActivity} also runs under unit-test mode, so the platform's own registration and
 * a test's are both live at once; a registration-local flag coalesced only against itself and left
 * the other free to schedule a second rebuild for the same change.
 */
public final class GrailsCustomFoldersRefreshListener implements ProjectActivity, DumbAware {

  private static final Key<AtomicBoolean> REFRESH_QUEUED = Key.create("GrailsCustomFoldersRefreshQueued");

  @Override
  public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(GrailsCustomFoldersProjectService.FoldersChangedListener.TOPIC,
                         () -> schedule(project));

    // Parented to the project: the application bus outlives it, so without the parent the
    // subscription would survive every close and refresh panes that are already gone.
    MessageBusConnection appConnection = ApplicationManager.getApplication().getMessageBus().connect(project);
    appConnection.subscribe(GrailsCustomFoldersAppService.GlobalFoldersChangedListener.TOPIC,
                            () -> schedule(project));

    // A configured folder that does not exist on disk is a placeholder in the pane, and a build that
    // later creates it (build/ is the everyday case) used to leave the placeholder on screen for
    // good: nothing else makes the pane re-read the application root, and the Grails pane's node chain
    // is not reachable by the VFS-driven update the Project pane runs on, so the folder's own create
    // event never reached it. "Reload from Disk" on the tool window does not help either — it
    // synchronizes the VFS and does not rebuild the pane.
    appConnection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkFileListener() {
      @Override
      public void after(@NotNull List<? extends VFileEvent> events) {
        Set<String> paths = null;
        for (VFileEvent event : events) {
          if (paths == null) paths = configuredFolderPaths(project);
          if (paths.contains(event.getPath())) {
            schedule(project);
            return;
          }
        }
      }
    });

    return null;
  }

  /**
   * Whether the changed path is one the Grails view renders a custom folder for. The Grails pane
   * reads a configured path against each Grails application root, so a path only counts when it is
   * exactly one of those; a file written *inside* a custom folder has a longer path and so costs
   * nothing here — a build writes hundreds of them under build/ and none of them match. Without that
   * exactness every file a build touched would rebuild the pane.
   */
  static boolean isConfiguredFolder(@NotNull Project project, @NotNull String changedPath) {
    return configuredFolderPaths(project).contains(changedPath);
  }

  private static @NotNull Set<String> configuredFolderPaths(@NotNull Project project) {
    List<String> folders = project.getService(GrailsCustomFoldersProjectService.class).getEffectiveFolders();
    if (folders.isEmpty()) return Set.of();
    Set<String> paths = new HashSet<>();
    for (GrailsApplication application : GrailsApplicationManager.getInstance(project).getApplications()) {
      String rootPath = application.getRoot().getPath();
      for (String folder : folders) {
        paths.add(rootPath + "/" + FolderPaths.normalize(folder));
      }
    }
    return paths;
  }

  private static void schedule(@NotNull Project project) {
    AtomicBoolean refreshQueued = flagFor(project);
    if (!refreshQueued.compareAndSet(false, true)) return;
    ApplicationManager.getApplication().invokeLater(() -> {
      refreshQueued.set(false);
      GrailsProjectViewPanes.refresh(project);
    }, ModalityState.defaultModalityState(), project.getDisposed());
  }

  private static @NotNull AtomicBoolean flagFor(@NotNull Project project) {
    AtomicBoolean flag = project.getUserData(REFRESH_QUEUED);
    if (flag == null) {
      flag = new AtomicBoolean();
      project.putUserData(REFRESH_QUEUED, flag);
    }
    return flag;
  }
}

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
package org.apache.grails.intellij.plugin.gradle;

import com.intellij.build.BuildContentManager;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.wm.ToolWindow;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.psi.search.FilenameIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.ui.EditorNotifications;
import com.intellij.util.PathUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.containers.ContainerUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.projectView.GrailsProjectViewPanes;
import org.apache.grails.intellij.plugin.structure.Grails9Projects;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Knows whether the Gradle import that Grails 3+ support depends on is usable, per linked Gradle
 * project, and drives the "Grails support unavailable" indicators.
 *
 * <p>{@link org.apache.grails.intellij.plugin.structure.impl.Grails3ApplicationProvider} recognises a
 * module as a Grails application only from the data a successful Gradle import produces. When that
 * import fails (a broken build script, an unresolvable dependency) or never ran, the plugin shows no
 * Grails view, console or GSP support at all, which reads as "the plugin is broken". This service
 * tells the difference, and three UI pieces ask it:
 * <ul>
 *   <li>the Grails project view pane, which lists such roots as
 *       {@link org.apache.grails.intellij.plugin.projectView.nodes.GrailsGradleSyncProblemNode}s,</li>
 *   <li>{@link org.apache.grails.intellij.plugin.editor.GrailsGradleSyncNotificationProvider}, a banner
 *       on files under such a {@code grails-app} folder,</li>
 *   <li>a one-shot balloon when an import fails for a project that has a {@code grails-app} folder.</li>
 * </ul>
 *
 * <p>The state comes from the resolve and import events of the current session
 * ({@link GrailsGradleSyncTaskListener}, {@link ProjectDataImportListener}) and, as the baseline for a
 * freshly opened project, from whether the external-system store holds a successful import for the
 * linked project at all. A project whose latest import failed but that still has data from an earlier
 * successful one keeps its Grails support with that data, so nothing is reported for it.
 */
public final class GrailsGradleSyncStatus implements Disposable {

  private static final String GRAILS_APP = "grails-app";
  private static final String NOTIFICATION_GROUP = "Grails Gradle import";
  private static final int MAX_ERROR_LENGTH = 200;

  public enum Reason {IMPORT_FAILED, NOT_IMPORTED}

  /** A linked Gradle project whose import cannot currently back Grails support. */
  public record BlockedProject(@NotNull String externalProjectPath, @NotNull Reason reason, @Nullable String errorMessage) {
    public @NotNull String getName() {
      String name = PathUtil.getFileName(externalProjectPath);
      return name.isEmpty() ? externalProjectPath : name;
    }

    public @NotNull String getShortDescription() {
      return GrailsBundle.message(reason == Reason.IMPORT_FAILED ? "gradle.sync.blocked.import.failed" : "gradle.sync.blocked.not.imported");
    }
  }

  /**
   * A directory with a {@code grails-app} child or a {@code .grails/project.yml} record that no provider recognises
   * because its Gradle project is blocked.
   */
  public record BlockedGrailsRoot(@NotNull VirtualFile root, @NotNull BlockedProject project) {
  }

  private final Project myProject;
  /** External project path to the first line of the Gradle error, or the empty string when unknown. */
  private final Map<String, String> myFailures = new ConcurrentHashMap<>();
  private final Set<String> mySucceeded = ConcurrentHashMap.newKeySet();
  private final Set<String> myInProgress = ConcurrentHashMap.newKeySet();
  private final Set<String> myNotified = ConcurrentHashMap.newKeySet();

  public GrailsGradleSyncStatus(@NotNull Project project) {
    myProject = project;
    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(ProjectDataImportListener.TOPIC, new ProjectDataImportListener() {
      @Override
      public void onImportFailed(String projectPath, @NotNull Throwable t) {
        if (projectPath != null) recordFailure(projectPath, t.getMessage());
      }

      @Override
      public void onImportFinished(String projectPath) {
        if (projectPath != null) recordSuccess(projectPath);
      }
    });
    connection.subscribe(DumbService.DUMB_MODE, new DumbService.DumbModeListener() {
      @Override
      public void exitDumbMode() {
        refreshIndicators();
      }
    });
  }

  public static @NotNull GrailsGradleSyncStatus getInstance(@NotNull Project project) {
    return project.getService(GrailsGradleSyncStatus.class);
  }

  @Override
  public void dispose() {
  }

  void syncStarted(@NotNull String externalProjectPath) {
    myInProgress.add(externalProjectPath);
    refreshIndicators();
  }

  void syncCancelled(@NotNull String externalProjectPath) {
    myInProgress.remove(externalProjectPath);
    refreshIndicators();
  }

  /** The Gradle resolve or data import of {@code externalProjectPath} failed with {@code errorMessage}. */
  public void recordFailure(@NotNull String externalProjectPath, @Nullable String errorMessage) {
    myInProgress.remove(externalProjectPath);
    mySucceeded.remove(externalProjectPath);
    myFailures.put(externalProjectPath, firstLine(errorMessage));
    refreshIndicators();
    notifyIfGrailsProject(externalProjectPath);
  }

  /** The Gradle import of {@code externalProjectPath} completed. */
  public void recordSuccess(@NotNull String externalProjectPath) {
    myInProgress.remove(externalProjectPath);
    myFailures.remove(externalProjectPath);
    myNotified.remove(externalProjectPath);
    mySucceeded.add(externalProjectPath);
    refreshIndicators();
  }

  /**
   * @return why the Gradle project at {@code externalProjectPath} cannot back Grails support right now, or
   * {@code null} when its import is fine or still running.
   */
  public @Nullable BlockedProject getBlockedProject(@NotNull String externalProjectPath) {
    if (myInProgress.contains(externalProjectPath)) return null;
    String failure = myFailures.get(externalProjectPath);
    if (failure != null) {
      return new BlockedProject(externalProjectPath, Reason.IMPORT_FAILED, failure.isEmpty() ? null : failure);
    }
    if (mySucceeded.contains(externalProjectPath) || myProject.isDisposed()) return null;
    ExternalProjectInfo info =
      ProjectDataManager.getInstance().getExternalProjectData(myProject, GradleConstants.SYSTEM_ID, externalProjectPath);
    if (info == null || info.getExternalProjectStructure() == null) {
      return new BlockedProject(externalProjectPath, Reason.NOT_IMPORTED, null);
    }
    if (info.getLastImportTimestamp() > info.getLastSuccessfulImportTimestamp()) {
      return new BlockedProject(externalProjectPath, Reason.IMPORT_FAILED, null);
    }
    return null;
  }

  /**
   * @return the blocked Gradle project whose root contains {@code fileOrDirectory} (the deepest one when
   * builds are nested), or {@code null} when no linked Gradle project covers it or that project is fine.
   */
  public @Nullable BlockedProject findBlockingProject(@NotNull VirtualFile fileOrDirectory) {
    String path = fileOrDirectory.getPath();
    String best = null;
    for (String candidate : knownProjectPaths()) {
      if (FileUtil.isAncestor(candidate, path, false) && (best == null || candidate.length() > best.length())) {
        best = candidate;
      }
    }
    return best == null ? null : getBlockedProject(best);
  }

  /**
   * Directories that are the root of a Grails project by their files, that {@link GrailsApplicationManager}
   * does not recognise and that a blocked Gradle project covers. A directory is such a root when it has a
   * {@code grails-app} child, or the record Grails 9 writes for a project ({@code .grails/project.yml}, see
   * {@link Grails9Projects}). Needs read access; empty while indexing.
   */
  public @NotNull List<BlockedGrailsRoot> findBlockedGrailsRoots() {
    ApplicationManager.getApplication().assertReadAccessAllowed();
    if (myProject.isDisposed() || DumbService.isDumb(myProject)) return List.of();
    if (!ContainerUtil.exists(knownProjectPaths(), path -> getBlockedProject(path) != null)) return List.of();

    GrailsApplicationManager manager = GrailsApplicationManager.getInstance(myProject);
    GlobalSearchScope scope = GlobalSearchScope.projectScope(myProject);
    Set<VirtualFile> roots = new LinkedHashSet<>();
    for (VirtualFile appDir : FilenameIndex.getVirtualFilesByName(GRAILS_APP, scope)) {
      if (appDir.isDirectory() && appDir.getParent() != null) roots.add(appDir.getParent());
    }
    for (VirtualFile record : FilenameIndex.getVirtualFilesByName(Grails9Projects.RECORD_FILE, scope)) {
      VirtualFile root = Grails9Projects.projectRootOfRecord(record);
      if (root != null) roots.add(root);
    }

    List<BlockedGrailsRoot> result = new ArrayList<>();
    for (VirtualFile root : roots) {
      if (manager.getApplicationByRoot(root) != null) continue;
      BlockedProject blocked = findBlockingProject(root);
      if (blocked != null) result.add(new BlockedGrailsRoot(root, blocked));
    }
    result.sort(Comparator.comparing(blockedRoot -> blockedRoot.root().getPath()));
    return result;
  }

  /** Reloads the Gradle project, which is what brings Grails support back once the build is fixed. */
  public void reloadGradleProject(@NotNull String externalProjectPath) {
    ExternalSystemUtil.refreshProject(externalProjectPath, new ImportSpecBuilder(myProject, GradleConstants.SYSTEM_ID));
  }

  /** Shows the Build tool window, where the platform reports the Gradle error itself. */
  public void showBuildToolWindow() {
    ToolWindow toolWindow = ToolWindowManager.getInstance(myProject).getToolWindow(BuildContentManager.TOOL_WINDOW_ID);
    if (toolWindow != null) toolWindow.show();
  }

  private @NotNull Set<String> knownProjectPaths() {
    Set<String> result = new HashSet<>(myFailures.keySet());
    result.addAll(myInProgress);
    result.addAll(mySucceeded);
    if (!myProject.isDisposed()) {
      for (ExternalProjectSettings settings : GradleSettings.getInstance(myProject).getLinkedProjectsSettings()) {
        result.add(settings.getExternalProjectPath());
      }
    }
    return result;
  }

  private void refreshIndicators() {
    if (myProject.isDisposed()) return;
    ApplicationManager.getApplication().invokeLater(() -> {
      EditorNotifications.getInstance(myProject).updateAllNotifications();
      if (!ApplicationManager.getApplication().isUnitTestMode()) {
        GrailsProjectViewPanes.showHide(myProject);
        GrailsProjectViewPanes.refresh(myProject);
      }
    }, myProject.getDisposed());
  }

  private void notifyIfGrailsProject(@NotNull String externalProjectPath) {
    if (ApplicationManager.getApplication().isUnitTestMode() || myProject.isDisposed()) return;
    if (!myNotified.add(externalProjectPath)) return;
    ReadAction.nonBlocking(() -> ContainerUtil.exists(findBlockedGrailsRoots(),
                                                      blockedRoot -> externalProjectPath.equals(blockedRoot.project().externalProjectPath())))
      .inSmartMode(myProject)
      .expireWith(this)
      .finishOnUiThread(ModalityState.nonModal(), blocksGrails -> {
        if (blocksGrails) {
          showNotification(externalProjectPath);
        }
        else {
          myNotified.remove(externalProjectPath);
        }
      })
      .submit(AppExecutorUtil.getAppExecutorService());
  }

  private void showNotification(@NotNull String externalProjectPath) {
    BlockedProject blocked = getBlockedProject(externalProjectPath);
    if (blocked == null) return;
    NotificationGroupManager.getInstance().getNotificationGroup(NOTIFICATION_GROUP)
      .createNotification(GrailsBundle.message("gradle.sync.blocked.notification.title"),
                          GrailsBundle.message("gradle.sync.blocked.notification.content", blocked.getName()),
                          NotificationType.WARNING)
      .addAction(NotificationAction.createSimpleExpiring(GrailsBundle.message("gradle.sync.blocked.action.reload"),
                                                         () -> reloadGradleProject(externalProjectPath)))
      .addAction(NotificationAction.createSimple(GrailsBundle.message("gradle.sync.blocked.action.show.build"),
                                                 this::showBuildToolWindow))
      .notify(myProject);
  }

  private static @NotNull String firstLine(@Nullable String message) {
    if (StringUtil.isEmptyOrSpaces(message)) return "";
    String line = message.strip();
    int eol = line.indexOf('\n');
    if (eol >= 0) line = line.substring(0, eol).strip();
    return StringUtil.shortenTextWithEllipsis(line, MAX_ERROR_LENGTH, 0);
  }
}

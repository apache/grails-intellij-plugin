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

import com.intellij.icons.AllIcons;
import com.intellij.ide.SelectInContext;
import com.intellij.ide.SelectInTarget;
import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import com.intellij.openapi.util.ActionCallback;
import com.intellij.openapi.util.io.FileAttributes;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.newvfs.events.VFileCreateEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileDeleteEvent;
import com.intellij.openapi.vfs.newvfs.events.VFileEvent;
import com.intellij.testFramework.PlatformTestUtil;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.config.GrailsConstants;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersAppService;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersProjectService;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;


/**
 * Drives the subscriber through the same entry point {@code postStartupActivity} uses, then asserts
 * that each of the three folder changes reaches the Grails pane.
 * <p>
 * The pane's rebuild is observed through a counting pane registered under
 * {@code GrailsProjectViewPanes#refresh}'s own id: a light fixture has no real Grails pane, so
 * {@code refresh} would be a no-op there and the test would pass without the subscriber doing
 * anything. Which panes were rebuilt is collected as a set of projects rather than a count, because
 * the application-bus connection survives the test that opened it.
 * <p>
 * "Every open project" is reached by each project holding its own application-bus subscription and
 * refreshing itself, not by one handler fanning out over the open-project list; what is asserted
 * here is that this project's pane is rebuilt and that nothing outside the open list is.
 */
public class GrailsCustomFoldersRefreshListenerTest extends GrailsTestCase {

  private final Set<Project> refreshed = new LinkedHashSet<>();
  private final List<Project> rebuilds = new ArrayList<>();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    ProjectView.getInstance(getProject())
      .addProjectPane(new CountingGrailsPane(getProject(), refreshed, rebuilds));
  }

  /** FR38(a): this project's own folder list changed, here from a settings-style write. */
  public void testProjectFoldersChangedRefreshesThisProjectsPane() {
    baseline();

    getProject().getService(GrailsCustomFoldersProjectService.class).setFolders(List.of("docs"));

    flush();
    assertEquals("the project topic refreshes this project's pane and nothing else",
                 Collections.singleton(getProject()), refreshed);
  }

  /** FR38(b): the cap changed, which shortens the effective list in every open project. */
  public void testMaxFoldersChangedRefreshesThisProjectsPane() {
    withGlobalDefaults(() -> {
      baseline();

      appService().setMaxFolders(2);

      flush();
      assertThisProjectsPaneRefreshedAndNothingOutsideTheOpenList();
    });
  }

  /** FR38(c): the global defaults changed, which changes the effective list in every project. */
  public void testGlobalFoldersChangedRefreshesThisProjectsPane() {
    withGlobalDefaults(() -> {
      baseline();

      appService().setFolders(Arrays.asList("docs", "src"));

      flush();
      assertThisProjectsPaneRefreshedAndNothingOutsideTheOpenList();
    });
  }

  /**
   * The application topic reaches this project through its own subscription, so every open project's
   * pane is rebuilt by its own handler rather than by one handler fanning out over
   * {@code ProjectManager}'s open-project list — the fan-out did N work per subscriber, N&sup2; tree
   * rebuilds for one Apply. A light fixture can open only one project, so what this test pins is
   * that this project's pane is rebuilt and that nothing outside the open list is; the two-window
   * case is recorded for {@code runIde} against FR38(g). The open list is read <em>after</em> the
   * flush, because it is process-wide state that other test classes leave as they find it.
   */
  public void testApplicationTopicReachesThisProjectThroughItsOwnSubscription() {
    withGlobalDefaults(() -> {
      baseline();

      appService().setMaxFolders(4);

      flush();
      assertThisProjectsPaneRefreshedAndNothingOutsideTheOpenList();
    });
  }

  /**
   * The reporter's bug: a configured folder that does not exist on disk renders as a placeholder, and
   * a build that later creates it — build/ is the everyday case — left the placeholder on screen for
   * good. Nothing else makes the pane re-read the application root, and the Grails pane's node chain
   * is not reachable by the VFS-driven update the Project pane runs on, so the folder's own create
   * event never reached it. The tool window's "Reload from Disk" does not help either: it
   * synchronizes the VFS and does not rebuild the pane.
   */
  public void testConfiguredFolderAppearingOnDiskRefreshesThePane() throws IOException {
    VirtualFile folder = configureFolderOnDisk("build");
    projectFolders("build");
    baseline();

    publish(created(folder.getParent(), folder.getName()));

    flush();
    assertTrue("a configured folder that appears on disk must rebuild the pane",
               rebuilds.contains(getProject()));
  }

  /** A folder that is deleted earlier and then rebuilt by a task is the same problem in reverse. */
  public void testConfiguredFolderDisappearingFromDiskRefreshesThePane() throws IOException {
    VirtualFile folder = configureFolderOnDisk("build");
    projectFolders("build");
    baseline();

    publish(deleted(folder));

    flush();
    assertTrue("a configured folder that disappears from disk must rebuild the pane",
               rebuilds.contains(getProject()));
  }

  /**
   * Exactness is what keeps this cheap: a build writes hundreds of files *inside* build/, and a
   * match on any prefix of a configured path would rebuild the pane for every one of them. Only the
   * configured path itself counts.
   */
  public void testAFileInsideAConfiguredFolderDoesNotRefreshThePane() throws IOException {
    VirtualFile folder = configureFolderOnDisk("build");
    projectFolders("build");
    baseline();

    publish(created(folder, "output.txt"));

    flush();
    assertTrue("a file written inside a configured folder is not the folder, so nothing rebuilds",
               refreshed.isEmpty());
  }

  /** And a directory that is not configured at all must be as cheap. */
  public void testAnUnconfiguredFileDoesNotRefreshThePane() throws IOException {
    VirtualFile folder = configureFolderOnDisk("src");
    projectFolders("build");
    baseline();

    publish(created(folder.getParent(), folder.getName()));

    flush();
    assertTrue("an unconfigured path must not rebuild the pane", refreshed.isEmpty());
  }

  /**
   * What the listener matches on, with no VFS in the way: a configured path is resolved against the
   * Grails application root, so the string that counts is the root path plus the normalized
   * configured path — not the configured path on its own, which is what made the project-relative
   * spelling miss in the first place.
   */
  public void testConfiguredFolderPathsAreTheApplicationRootOnes() throws IOException {
    VirtualFile folder = configureFolderOnDisk("build");
    projectFolders("build", "docs/");
    String root = applicationRootPath();

    assertTrue("a configured path is matched against the application root",
               GrailsCustomFoldersRefreshListener.isConfiguredFolder(getProject(), root + "/build"));
    assertTrue("and the path is normalized, so a trailing slash does not change it",
               GrailsCustomFoldersRefreshListener.isConfiguredFolder(getProject(), root + "/docs"));
    assertFalse("a file inside a configured folder is not the folder",
                GrailsCustomFoldersRefreshListener.isConfiguredFolder(getProject(), root + "/build/output.txt"));
    assertFalse("the configured path on its own is not what the view resolves against",
                GrailsCustomFoldersRefreshListener.isConfiguredFolder(getProject(), "build"));

    publish(created(folder.getParent(), folder.getName()));
    flush();
    assertTrue("the folder was configured, so the event above reached the pane",
               rebuilds.contains(getProject()));
  }

  /**
   * FR38 and the coalescing guard: one Apply from the settings dialog writes the global defaults and
   * this project's own folders, so both buses fire for one click. The subscriber must rebuild the
   * pane once, not once per field written — the second event is absorbed by the rebuild the first one
   * already queued. Without the guard this fails with two rebuilds.
   */
  public void testOneApplyRebuildsThePaneOnce() {
    withGlobalDefaults(() -> {
      baseline();

      appService().setGlobalDefaults(List.of("global/one"), 4);
      getProject().getService(GrailsCustomFoldersProjectService.class).setFolders(List.of("project/two"));

      flush();
      assertEquals("one Apply must rebuild the pane once, not once per bus that published",
                   1, rebuilds.size());
      assertTrue("and the rebuild is this project's pane", rebuilds.contains(getProject()));
    });
  }

  /**
   * Two genuinely separate changes are not one event, so they are not absorbed into each other: the
   * flag is cleared as the queued rebuild starts, so an edit after a flush is its own rebuild. This
   * is what stops the coalescing from swallowing real changes.
   */
  public void testSeparateChangesAreNotCoalesced() {
    withGlobalDefaults(() -> {
      baseline();

      appService().setFolders(List.of("global/one"));
      flush();

      appService().setFolders(List.of("global/one", "global/two"));
      flush();

      assertEquals("each change gets its own rebuild, so the second one is not lost",
                   2, rebuilds.size());
    });
  }

  /**
   * The pane was rebuilt for this project, and for no project that was not open. "Every open
   * project" cannot be asserted from a light fixture, which can open only one: the platform's
   * open-project list is process-wide state that other test classes populate as they set up and tear
   * down, and a project they left behind has no Grails pane of its own to rebuild. Asserting equality
   * with that list would fail whenever any other class left a project in it, which is exactly the
   * flakiness this avoids; the platform's list cannot be narrowed from here either.
   */
  private void assertThisProjectsPaneRefreshedAndNothingOutsideTheOpenList() {
    Set<Project> open = openProjectSet();
    assertTrue("the pane must be rebuilt for the project that is open",
               refreshed.contains(getProject()));
    assertTrue("no pane outside the open-project list may be rebuilt, or a subscription reached a project it does not own",
               open.containsAll(refreshed));
    assertTrue("the listener must rebuild at least one pane, or it proved nothing",
               !refreshed.isEmpty());
  }

  /**
   * The open projects the listener could refresh, including any an earlier test class left behind:
   * the listener cannot narrow the platform's list, so the assertion cannot either.
   */
  private static @NotNull Set<Project> openProjectSet() {
    Set<Project> open = new LinkedHashSet<>(Arrays.asList(ProjectManager.getInstance().getOpenProjects()));
    open.removeIf(Project::isDisposed);
    return open;
  }

  private static GrailsCustomFoldersAppService appService() {
    return GrailsCustomFoldersAppService.getStaticInstance();
  }

  /**
   * Runs {@code body} with the global defaults left as it found them. They are application state, so
   * leaving them mutated here would change the effective folders of every later test in the run.
   */
  private static void withGlobalDefaults(@NotNull Runnable body) {
    GrailsCustomFoldersAppService appService = appService();
    List<String> original = new ArrayList<>(appService.getFolders());
    int originalMax = appService.getMaxFolders();
    try {
      body.run();
    }
    finally {
      appService.setFolders(original);
      appService.setMaxFolders(originalMax);
    }
  }

  private void baseline() {
    // Register the subscriber and drop whatever the fixture already put in the EDT queue, so the
    // assertions count only what the change under test published.
    new GrailsCustomFoldersRefreshListener().execute(getProject(), continuation());
    flush();
    refreshed.clear();
    rebuilds.clear();
  }


  /** The application root the fixture registers, which is the base a configured path resolves against. */
  private @NotNull String applicationRootPath() {
    List<GrailsApplication> applications =
      new ArrayList<>(GrailsApplicationManager.getInstance(getProject()).getApplications());
    assertEquals("the fixture must register one Grails application", 1, applications.size());
    return applications.get(0).getRoot().getPath();
  }

  private void projectFolders(@NotNull String... folders) {
    getProject().getService(GrailsCustomFoldersProjectService.class).setFolders(List.of(folders));
  }

  /** Creates the directory on disk, so its path is one the VFS would report. */
  private @NotNull VirtualFile configureFolderOnDisk(@NotNull String path) throws IOException {
    VirtualFile folder = myFixture.getTempDirFixture().findOrCreateDir(path);
    assertNotNull(folder);
    return folder;
  }

  /** Attributes for a directory, which is all the event's path computation needs. */
  private static @NotNull FileAttributes directoryAttributes() {
    return new FileAttributes(true, false, false, false, 0L, 0L, false);
  }

  /** The event shape the VFS fires for a created directory, so the listener sees the input it will get. */
  private static @NotNull VFileEvent created(@NotNull VirtualFile parent, @NotNull String childName) {
    return new VFileCreateEvent(GrailsCustomFoldersRefreshListenerTest.class, parent,
                                childName, true, directoryAttributes(), null, null);
  }

  /** The event shape the VFS fires for a deleted file. */
  private static @NotNull VFileEvent deleted(@NotNull VirtualFile file) {
    return new VFileDeleteEvent(GrailsCustomFoldersRefreshListenerTest.class, file);
  }

  /**
   * Publishes one event the way the platform dispatches a batch. The write action is not incidental:
   * {@code BulkFileListener} is invoked under one, and without it the platform's threading assertion
   * fires inside the listener instead of the listener running.
   */
  private static void publish(@NotNull VFileEvent event) {
    ApplicationManager.getApplication().runWriteAction(() ->
      ApplicationManager.getApplication().getMessageBus()
        .syncPublisher(VirtualFileManager.VFS_CHANGES)
        .after(List.of(event)));
  }

  private static void flush() {
    ApplicationManager.getApplication().invokeAndWait(
      PlatformTestUtil::dispatchAllInvocationEventsInIdeEventQueue);
  }

  /** The subscriber is driven, not re-implemented, so {@code execute} needs the platform's shape. */
  private static @NotNull Continuation<Unit> continuation() {
    return new Continuation<Unit>() {
      @Override
      public @NotNull CoroutineContext getContext() {
        return EmptyCoroutineContext.INSTANCE;
      }

      @Override
      public void resumeWith(@NotNull Object result) {
      }
    };
  }

  /**
   * Stands in for the Grails pane under {@code GrailsProjectViewPanes#refresh}: it records the
   * projects whose panes were asked to rebuild, without building a tree.
   */
  private static final class CountingGrailsPane extends AbstractProjectViewPane {

    private final @NotNull Set<Project> refreshed;
    private final @NotNull List<Project> rebuilds;

    private CountingGrailsPane(@NotNull Project project,
                               @NotNull Set<Project> refreshed,
                               @NotNull List<Project> rebuilds) {
      super(project);
      this.refreshed = refreshed;
      this.rebuilds = rebuilds;
    }

    @Override
    public @NotNull String getTitle() {
      return "Grails";
    }

    @Override
    public @NotNull Icon getIcon() {
      return AllIcons.Nodes.Package;
    }

    @Override
    public int getWeight() {
      return 1;
    }

    @Override
    public @NotNull String getId() {
      return GrailsConstants.GRAILS;
    }

    @Override
    public @NotNull JComponent createComponent() {
      return new JPanel();
    }

    @Override
    public @NotNull ActionCallback updateFromRoot(boolean alreadyVisible) {
      refreshed.add(myProject);
      rebuilds.add(myProject);
      return ActionCallback.DONE;
    }

    /** Never selected into: the pane exists to be counted by the subscriber under test. */
    @Override
    public @NotNull SelectInTarget createSelectInTarget() {
      return new SelectInTarget() {
        @Override
        public String toString() {
          return "Grails";
        }

        @Override
        public boolean canSelect(@NotNull SelectInContext context) {
          return false;
        }

        @Override
        public void selectIn(@NotNull SelectInContext context, boolean requestFocus) {
        }
      };
    }

    @Override
    public void select(@NotNull Object value, @NotNull VirtualFile file, boolean requestFocus) {
    }
  }
}

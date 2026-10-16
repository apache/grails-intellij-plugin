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

package org.apache.grails.intellij.plugin.config;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.config.GrailsConstants;
import org.apache.grails.intellij.plugin.projectView.GrailsProjectViewPanes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GrailsCustomFoldersProjectServiceTest extends GrailsTestCase {

  public void testSetAndGetFolders() {
    GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
    List<String> folders = Arrays.asList("src/main/groovy", "src/test/groovy", "grails-app/domain");
    service.setFolders(folders);
    assertEquals(folders, service.getFolders());
  }

  public void testBlankLinesAreFiltered() {
    GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
    service.setFolders(Arrays.asList("src/main/groovy", "", "   ", "grails-app/domain"));
    assertEquals(Arrays.asList("src/main/groovy", "grails-app/domain"), service.getFolders());
  }

  /**
   * The cap limits what the view renders, not what is stored, so a stored folder must survive the
   * cap being lowered. {@link #getEffectiveFolders()} is where the truncation happens.
   */
  public void testFoldersAreNotTruncatedOnSave() {
    GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
    service.setFolders(Arrays.asList("a", "b", "c", "d"));
    assertEquals(Arrays.asList("a", "b", "c", "d"), service.getFolders());
  }

  public void testPersistenceRoundTrip() {
    GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
    service.setFolders(Arrays.asList("src/main/groovy", "grails-app/domain"));
    GrailsCustomFoldersProjectService.StateHolder state = service.getState();

    GrailsCustomFoldersProjectService restored = new GrailsCustomFoldersProjectService(getProject());
    restored.loadState(state);
    assertEquals(service.getFolders(), restored.getFolders());
  }

  public void testGetEffectiveFoldersCombinesGlobalAndProjectFolders() {
    withGlobalFolders(Arrays.asList("global/folder"), () -> {
      GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
      service.setFolders(Arrays.asList("project/folder"));
      assertEquals(Arrays.asList("project/folder", "global/folder"), service.getEffectiveFolders());
    });
  }

  public void testGetEffectiveFoldersDeduplicatesAcrossAreas() {
    withGlobalFolders(Arrays.asList("grails-app"), () -> {
      GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
      service.setFolders(Arrays.asList("grails-app/", "other"));
      assertEquals(Arrays.asList("grails-app/", "other"), service.getEffectiveFolders());
    });
  }

  public void testGetEffectiveFoldersFallsBackToGlobalDefaults() {
    withGlobalFolders(Arrays.asList("global/folder1", "global/folder2"), () -> {
      GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
      assertEquals(Arrays.asList("global/folder1", "global/folder2"), service.getEffectiveFolders());
    });
  }

  /** The cap is the setting, not a constant: lowering it shortens what the view shows. */
  public void testGetEffectiveFoldersReadsTheCapFromTheApplicationService() {
    withGlobalFolders(Arrays.asList("global1", "global2"), 2, () -> {
      GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
      service.setFolders(Arrays.asList("custom1", "custom2", "custom3"));
      assertEquals("the cap comes from the app service, so three project folders yield two",
                   Arrays.asList("custom1", "custom2"), service.getEffectiveFolders());
    });
  }

  /**
   * The cap binds only at render time: all ten project folders stay stored, and the view shows the
   * first two.
   */
  public void testTenProjectFoldersRenderOnlyTheFirstTwo() {
    List<String> ten = Arrays.asList("f1", "f2", "f3", "f4", "f5", "f6", "f7", "f8", "f9", "f10");
    withGlobalFolders(Arrays.asList(), 2, () -> {
      GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
      service.setFolders(ten);
      assertEquals(ten, service.getFolders());
      assertEquals(Arrays.asList("f1", "f2"), service.getEffectiveFolders());
    });
  }

  /** The cap counts unique folders across both areas, so project folders fill it first. */
  public void testCombineKeepsProjectFoldersFirstUnderTheCap() {
    assertEquals(Arrays.asList("custom1", "custom2", "global1"),
                 GrailsCustomFoldersProjectService.combine(Arrays.asList("custom1", "custom2"),
                                                           Arrays.asList("global1", "global2"), 3));
  }

  /** Any number of global folders still yields at most the cap in total. */
  public void testCombineLimitsGlobalOnlyFolders() {
    assertEquals(Arrays.asList("global1", "global2"),
                 GrailsCustomFoldersProjectService.combine(Arrays.<String>asList(),
                                                           Arrays.asList("global1", "global2", "global3"), 2));
  }

  /** A folder in both areas is rendered once, keeping the project's spelling of it. */
  public void testDuplicateAcrossAreasKeepsProjectSpelling() {
    withGlobalFolders(Arrays.asList("docs"), () -> {
      GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
      service.setFolders(Arrays.asList("docs/"));
      assertEquals(Arrays.asList("docs/"), service.getEffectiveFolders());
    });
  }

  /** The cap counts unique folders, so duplicates must not crowd out a real one. */
  public void testCapCountsUniqueFoldersOnly() {
    withGlobalFolders(Arrays.asList("docs/", "docs", "other"), () -> {
      GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
      assertEquals(Arrays.asList("docs/", "other"), service.getEffectiveFolders());
    });
  }

  /** A cap of zero would render nothing, so the effective list is empty rather than all of them. */
  public void testCombineWithNoCapRendersNothing() {
    assertEquals(Arrays.<String>asList(),
                 GrailsCustomFoldersProjectService.combine(Arrays.asList("a"),
                                                           Arrays.asList("b"), 0));
  }

  /** Runs {@code body} with the global defaults temporarily set, restoring them afterwards. */
  private void withGlobalFolders(@NotNull List<String> folders, @NotNull Runnable body) {
    withGlobalFolders(folders, GrailsCustomFoldersAppService.getStaticInstance().getMaxFolders(), body);
  }

  private void withGlobalFolders(@NotNull List<String> folders, int maxFolders, @NotNull Runnable body) {
    GrailsCustomFoldersAppService appService = GrailsCustomFoldersAppService.getStaticInstance();
    List<String> original = new ArrayList<>(appService.getFolders());
    int originalMax = appService.getMaxFolders();
    try {
      appService.setFolders(folders);
      appService.setMaxFolders(maxFolders);
      body.run();
    }
    finally {
      appService.setFolders(original);
      appService.setMaxFolders(originalMax);
    }
  }

  /**
   * Changing the folders publishes the project-bus event that the pane-refresh subscriber listens
   * for. The refresh itself is the subscriber's job
   * ({@link GrailsCustomFoldersRefreshListenerTest}); the service only publishes.
   */
  public void testSetFoldersPublishesChangeEvent() {
    GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
    boolean[] eventReceived = {false};

    MessageBusConnection connection = getProject().getMessageBus().connect(getTestRootDisposable());
    connection.subscribe(GrailsCustomFoldersProjectService.FoldersChangedListener.TOPIC,
                         () -> eventReceived[0] = true);

    service.setFolders(Arrays.asList("src/main/groovy"));

    assertTrue("setFolders should publish a FoldersChangedListener event", eventReceived[0]);
  }

  /**
   * Verifies that GrailsProjectViewPanes.refresh() is safe to call when the Grails pane
   * doesn't exist (e.g., no Grails applications in the project).
   */
  public void testRefreshIsNoOpWhenGrailsPaneMissing() {
    ProjectView projectView = ProjectView.getInstance(getProject());
    AbstractProjectViewPane grailsPane = projectView.getProjectViewPaneById(GrailsConstants.GRAILS);

    if (grailsPane != null) {
      // If the pane exists, remove it temporarily
      projectView.removeProjectPane(grailsPane);
      try {
        // Should not throw
        GrailsProjectViewPanes.refresh(getProject());
      }
      finally {
        projectView.addProjectPane(grailsPane);
      }
    }
    else {
      // No pane exists; refresh should be a no-op
      GrailsProjectViewPanes.refresh(getProject());
    }
  }

  public void testDuplicatePathsAreDeduplicated() {
    GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
    service.setFolders(Arrays.asList("grails-app", "grails-app/", "grails-app\\"));
    assertEquals(Arrays.asList("grails-app"), service.getFolders());
  }

  public void testDuplicatePathsWithDotSlashPrefix() {
    GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
    service.setFolders(Arrays.asList("src/main", "./src/main", ".\\src/main"));
    assertEquals(Arrays.asList("src/main"), service.getFolders());
  }

  public void testFirstInstanceIsKept() {
    GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
    service.setFolders(Arrays.asList("grails-app/", "grails-app", "other"));
    assertEquals(Arrays.asList("grails-app/", "other"), service.getFolders());
  }

  public void testAbsolutePathAndRelativePathAreNotDuplicates() {
    GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
    service.setFolders(Arrays.asList("grails-app", "/grails-app"));
    assertEquals(Arrays.asList("grails-app", "/grails-app"), service.getFolders());
  }

  /**
   * The cap never truncated the store, so every unique folder is kept however many spellings of it
   * were typed: four here, not three.
   */
  public void testDeduplicationIsNotBoundedByTheCap() {
    GrailsCustomFoldersProjectService service = new GrailsCustomFoldersProjectService(getProject());
    service.setFolders(Arrays.asList("a", "a/", "b", "b/", "c", "c/", "d", "d/"));
    assertEquals(Arrays.asList("a", "b", "c", "d"), service.getFolders());
  }
}

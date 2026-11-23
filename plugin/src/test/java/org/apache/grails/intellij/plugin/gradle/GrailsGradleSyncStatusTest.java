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

import com.intellij.openapi.vfs.VirtualFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.gradle.GrailsGradleSyncStatus.BlockedGrailsRoot;
import org.apache.grails.intellij.plugin.gradle.GrailsGradleSyncStatus.BlockedProject;
import org.apache.grails.intellij.plugin.gradle.GrailsGradleSyncStatus.Reason;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class GrailsGradleSyncStatusTest extends GrailsTestCase {

  private GrailsGradleSyncStatus status() {
    return GrailsGradleSyncStatus.getInstance(getProject());
  }

  /** {@code relativePath} must contain a {@code grails-app} segment; returns that segment's parent. */
  private @NotNull VirtualFile createGrailsAppUnder(@NotNull String appDir) {
    VirtualFile file = myFixture.addFileToProject(appDir + "/grails-app/conf/application.yml", "grails: {}").getVirtualFile();
    return file.getParent().getParent().getParent();
  }

  public void testFailedImportBlocksTheProjectUntilTheNextSuccess() {
    status().recordFailure("/work/app", "Build file '/work/app/build.gradle' line: 3\nCould not find method foo()");

    BlockedProject blocked = status().getBlockedProject("/work/app");
    assertNotNull(blocked);
    assertEquals(Reason.IMPORT_FAILED, blocked.reason());
    assertEquals("only the first line of the Gradle error is kept", "Build file '/work/app/build.gradle' line: 3", blocked.errorMessage());
    assertEquals("app", blocked.getName());

    status().recordSuccess("/work/app");
    assertNull(status().getBlockedProject("/work/app"));
  }

  public void testARunningImportIsNotReported() {
    status().recordFailure("/work/app", "boom");
    status().syncStarted("/work/app");
    assertNull("a project being re-imported must not be flagged while the import runs", status().getBlockedProject("/work/app"));

    status().recordFailure("/work/app", "still broken");
    BlockedProject blocked = status().getBlockedProject("/work/app");
    assertNotNull(blocked);
    assertEquals("still broken", blocked.errorMessage());
  }

  public void testAProjectWithoutAnyImportDataIsReportedAsNotImported() {
    status().syncStarted("/work/never");
    status().syncCancelled("/work/never");

    BlockedProject blocked = status().getBlockedProject("/work/never");
    assertNotNull(blocked);
    assertEquals(Reason.NOT_IMPORTED, blocked.reason());
    assertNull(blocked.errorMessage());
  }

  public void testFindBlockingProjectPicksTheDeepestCoveringBuild() {
    VirtualFile appRoot = createGrailsAppUnder("build/app");
    VirtualFile outerRoot = appRoot.getParent();
    status().recordFailure(outerRoot.getPath(), "outer");
    status().recordFailure(appRoot.getPath(), "inner");

    BlockedProject blocked = status().findBlockingProject(appRoot.findFileByRelativePath("grails-app/conf/application.yml"));
    assertNotNull(blocked);
    assertEquals(appRoot.getPath(), blocked.externalProjectPath());
    assertEquals("inner", blocked.errorMessage());

    assertNull("a file no linked Gradle project covers is nobody's problem",
               status().findBlockingProject(myFixture.addFileToProject("elsewhere/x.txt", "").getVirtualFile()));
  }

  public void testBlockedGrailsRootsAreUnrecognisedGrailsAppParentsUnderABrokenBuild() {
    VirtualFile appRoot = createGrailsAppUnder("app");
    createGrailsAppUnder("healthy");
    status().recordFailure(appRoot.getPath(), "Could not resolve org.apache.grails:grails-core");

    List<BlockedGrailsRoot> blockedRoots = status().findBlockedGrailsRoots();
    assertEquals(blockedRoots.toString(), 1, blockedRoots.size());
    assertEquals(appRoot, blockedRoots.get(0).root());
    assertEquals(Reason.IMPORT_FAILED, blockedRoots.get(0).project().reason());
    assertEquals("Could not resolve org.apache.grails:grails-core", blockedRoots.get(0).project().errorMessage());

    status().recordSuccess(appRoot.getPath());
    assertEmpty("a successful import clears the indicator", status().findBlockedGrailsRoots());
  }

  public void testTheRecordOfAGrails9ProjectMarksItsRootToo() {
    VirtualFile recordOnly = myFixture.addFileToProject("generated/.grails/project.yml", "format: 1\n").getVirtualFile().getParent().getParent();
    VirtualFile both = createGrailsAppUnder("shop");
    myFixture.addFileToProject("shop/.grails/project.yml", "format: 1\n");
    VirtualFile notARecord = myFixture.addFileToProject("docs/project.yml", "").getVirtualFile().getParent();
    for (VirtualFile root : List.of(recordOnly, both, notARecord)) {
      status().recordFailure(root.getPath(), "Plugin [id: 'org.apache.grails.gradle.grails-web'] was not found");
    }

    List<VirtualFile> roots = status().findBlockedGrailsRoots().stream().map(BlockedGrailsRoot::root).toList();
    assertEquals("a root with grails-app and the record is listed once, and a project.yml outside .grails marks nothing",
                 List.of(recordOnly, both), roots);
  }

  public void testNothingIsReportedWhenNoBuildIsBroken() {
    createGrailsAppUnder("app");
    assertEmpty(status().findBlockedGrailsRoots());
  }
}

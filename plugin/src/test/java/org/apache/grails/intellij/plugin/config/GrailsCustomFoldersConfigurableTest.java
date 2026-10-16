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

import com.intellij.openapi.vfs.VirtualFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;


/**
 * The page's pure helpers take lists of paths, so they are driven here with no settings dialog in
 * the way: the preview merge, the over-cap warning, the description and the path validation
 * {@code apply()} performs.
 */
public class GrailsCustomFoldersConfigurableTest extends GrailsTestCase {

  public void testCombineKeepsThisProjectsFoldersFirst() {
    assertEquals(Arrays.asList("p1", "p2", "g1"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("p1", "p2"),
                                                         Arrays.asList("g1"), 10));
  }

  public void testCombineDeduplicatesExactDuplicates() {
    assertEquals(Arrays.asList("a", "b"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("a", "a", "b"),
                                                         Collections.<String>emptyList(), 10));
  }

  public void testCombineDeduplicatesTrailingSlash() {
    assertEquals(Arrays.asList("a", "b"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("a", "a/", "b"),
                                                         Collections.<String>emptyList(), 10));
  }

  public void testCombineDeduplicatesDotSlashPrefix() {
    assertEquals(Arrays.asList("a", "b"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("a", "./a", "b"),
                                                         Collections.<String>emptyList(), 10));
  }

  public void testCombineDeduplicatesBackslash() {
    assertEquals(Arrays.asList("a", "b"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("a", "a\\", "b"),
                                                         Collections.<String>emptyList(), 10));
  }

  public void testCombineKeepsTheFirstSpelling() {
    assertEquals(Arrays.asList("a/", "b"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("a/", "a", "b"),
                                                         Collections.<String>emptyList(), 10));
  }

  public void testCombineDoesNotDeduplicateRelativeAgainstAbsolute() {
    assertEquals(Arrays.asList("a", "/a"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("a", "/a"),
                                                         Collections.<String>emptyList(), 10));
  }

  public void testCombineSkipsBlankRows() {
    assertEquals(Arrays.asList("a", "b"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("a", "", "   ", "b"),
                                                         Collections.<String>emptyList(), 10));
  }

  public void testCombineDropsDuplicatesAcrossTheTwoLists() {
    assertEquals(Arrays.asList("docs/", "other"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("docs/", "other"),
                                                         Arrays.asList("docs"), 10));
  }

  public void testCombineTruncatesToTheCap() {
    assertEquals(Arrays.asList("a", "b"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("a", "b", "c"),
                                                         Arrays.asList("x", "y", "z"), 2));
  }

  public void testCombineTruncatesToACapOfOne() {
    assertEquals(Collections.singletonList("a"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("a", "b"),
                                                         Arrays.asList("g"), 1));
  }

  public void testCombineKeepsTheProjectFoldersWhenTheCapBinds() {
    assertEquals(Arrays.asList("c1", "c2", "g1"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("c1", "c2"),
                                                         Arrays.asList("g1", "g2"), 3));
  }

  public void testCombineKeepsEveryFolderUnderAGenerousCap() {
    assertEquals(Arrays.asList("a", "b", "c", "x", "y", "z"),
                 GrailsCustomFoldersConfigurable.combine(Arrays.asList("a", "b", "c"),
                                                         Arrays.asList("x", "y", "z"), 20));
  }

  public void testCombineOfEmptyListsIsEmpty() {
    assertTrue(GrailsCustomFoldersConfigurable.combine(Collections.<String>emptyList(),
                                                       Collections.<String>emptyList(), 10).isEmpty());
  }

  public void testOverCapWarningNoWarningWhenWithinLimit() {
    assertEquals("", GrailsCustomFoldersConfigurable.overCapWarning(Arrays.asList("a", "b"),
                                                                   Arrays.asList("c"), 3));
  }

  /** Three in total is exactly the cap, which is not over it. */
  public void testOverCapWarningNoWarningAtExactlyTheCap() {
    assertEquals("", GrailsCustomFoldersConfigurable.overCapWarning(Arrays.asList("a", "b", "c"),
                                                                   Collections.<String>emptyList(), 3));
  }

  public void testOverCapWarningNoWarningAtTheCombinedLimit() {
    assertEquals("", GrailsCustomFoldersConfigurable.overCapWarning(Arrays.asList("a", "b"),
                                                                   Arrays.asList("c"), 3));
  }

  public void testOverCapWarningNoWarningUnderAGenerousCap() {
    assertEquals("", GrailsCustomFoldersConfigurable.overCapWarning(Arrays.asList("a", "b", "c"),
                                                                   Arrays.asList("x", "y", "z"), 20));
  }

  public void testOverCapWarningWarnsWhenTheProjectListExceedsTheCap() {
    String warning = GrailsCustomFoldersConfigurable.overCapWarning(Collections.<String>emptyList(),
                                                                    Arrays.asList("a", "b", "c", "d"), 3);
    assertTrue(warning.contains("4"));
    assertTrue(warning.contains("3"));
  }

  public void testOverCapWarningWarnsWhenTheGlobalListExceedsTheCap() {
    String warning = GrailsCustomFoldersConfigurable.overCapWarning(Arrays.asList("a", "b", "c", "d", "e"),
                                                                    Collections.<String>emptyList(), 3);
    assertTrue(warning.contains("5"));
    assertTrue(warning.contains("3"));
  }

  public void testOverCapWarningCountsFoldersAcrossBothLists() {
    String warning = GrailsCustomFoldersConfigurable.overCapWarning(Arrays.asList("a", "b", "c", "d"),
                                                                    Arrays.asList("x", "y", "z", "w"), 3);
    assertTrue("the two lists contribute four each", warning.contains("8"));
    assertTrue(warning.contains("3"));
  }

  /** Two folders here plus two there is four in total, over the cap though neither list is. */
  public void testOverCapWarningCombinedTotalOverTheCap() {
    String warning = GrailsCustomFoldersConfigurable.overCapWarning(Arrays.asList("a", "b"),
                                                                    Arrays.asList("c", "d"), 3);
    assertTrue(warning.contains("4"));
    assertTrue(warning.contains("3"));
  }

  /** The warning names the count of unique folders and the cap it is over. */
  public void testOverCapWarningNamesTheUniqueCountAndTheCap() {
    String warning = GrailsCustomFoldersConfigurable.overCapWarning(Arrays.asList("a", "b", "c"),
                                                                    Arrays.asList("x", "y", "z"), 2);
    assertTrue(warning.contains("6 unique folders"));
    assertTrue(warning.contains("Only the first 2"));
  }

  public void testOverCapWarningCountsUniqueFolders() {
    String warning = GrailsCustomFoldersConfigurable.overCapWarning(Collections.<String>emptyList(),
                                                                    Arrays.asList("a", "a/", "b", "c", "d"), 3);
    assertTrue(warning.contains("4"));
    assertTrue(warning.contains("3"));
  }

  public void testOverCapWarningIgnoresDuplicates() {
    assertEquals("", GrailsCustomFoldersConfigurable.overCapWarning(Collections.<String>emptyList(),
                                                                    Arrays.asList("tasks/", "tasks", "other"), 3));
  }

  public void testOverCapWarningIgnoresBlankRows() {
    assertEquals("", GrailsCustomFoldersConfigurable.overCapWarning(Arrays.asList("a", "", "  ", "b"),
                                                                    Collections.<String>emptyList(), 3));
  }

  public void testOverCapWarningEmptyInput() {
    assertEquals("", GrailsCustomFoldersConfigurable.overCapWarning(Collections.<String>emptyList(),
                                                                    Collections.<String>emptyList(), 3));
  }

  public void testDescriptionStatesTheApplicationRootBase() {
    assertTrue(GrailsCustomFoldersConfigurable.description(6).contains("resolved against the Grails application root"));
  }

  public void testDescriptionDropsTheAbsolutePathClaim() {
    assertFalse(GrailsCustomFoldersConfigurable.description(6).contains("absolute paths are used as-is"));
    assertTrue(GrailsCustomFoldersConfigurable.description(6).contains("stay inside it"));
  }

  public void testDescriptionInterpolatesTheCap() {
    assertTrue(GrailsCustomFoldersConfigurable.description(20).contains("at most 20"));
    assertFalse(GrailsCustomFoldersConfigurable.description(20).contains("at most 3"));
  }

  public void testAbsolutePathIsRejected() {
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason("/etc", null));
  }

  public void testWindowsDrivePathIsRejected() {
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason("C:\\grails-app", null));
  }

  public void testUncPathIsRejected() {
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason("\\\\server\\share", null));
  }

  /**
   * Rows are validated as typed, so a stray leading space must not buy a path the trimmed form would
   * be refused: {@code normalize} trims, and the absolute-path test has to read what the other two
   * checks read. A Windows path needs the same treatment — it has no {@code ..} to be caught by the
   * escaping test, and {@code findFileByRelativePath} skips the empty first token of {@code C:/...},
   * so it resolves to nothing and would sit in the list as a permanent placeholder.
   */
  public void testAbsolutePathIsRejectedWhateverSurroundingWhitespaceItCarries() {
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason("  /etc", null));
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason("\t/etc", null));
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason("/etc  ", null));
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason(" C:\\grails-app", null));
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason(" \\\\server\\share ", null));
  }

  /** The same rows are refused when the application root is known, not only on shape alone. */
  public void testWhitespacePaddedAbsolutePathIsRejectedAgainstTheApplicationRoot() throws IOException {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("app");
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason("  /etc", root));
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason(" C:\\grails-app", root));
  }

  /** A padded path that is genuinely relative is still accepted, so the trimming is not over-eager. */
  public void testWhitespaceAroundARelativePathIsNotAReservation() throws IOException {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("app");
    myFixture.getTempDirFixture().findOrCreateDir("app/docs");
    assertNull(GrailsCustomFoldersConfigurable.invalidPathReason("  docs  ", root));
    assertNull(GrailsCustomFoldersConfigurable.invalidPathReason("  docs  ", null));
  }

  public void testPathEscapingTheApplicationRootIsRejected() {
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason("../outside", null));
    assertNotNull(GrailsCustomFoldersConfigurable.invalidPathReason("docs/../../outside", null));
  }

  public void testPathThatReturnsIntoTheApplicationRootIsAccepted() {
    assertNull(GrailsCustomFoldersConfigurable.invalidPathReason("docs/../readme", null));
    assertNull(GrailsCustomFoldersConfigurable.invalidPathReason("grails-app\\i18n", null));
  }

  public void testPathInsideTheApplicationRootIsAccepted() throws IOException {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("app");
    myFixture.getTempDirFixture().findOrCreateDir("app/docs");
    assertNull(GrailsCustomFoldersConfigurable.invalidPathReason("docs", root));
  }

  /** A path that does not resolve yet is left alone: it becomes a placeholder in the view. */
  public void testUnresolvedPathIsNotRejected() throws IOException {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("app");
    assertNull(GrailsCustomFoldersConfigurable.invalidPathReason("docs/not-created-yet", root));
  }

  public void testInvalidPathReasonNamesTheOffendingPath() {
    assertEquals("must be relative to the Grails application root, not an absolute path",
                 GrailsCustomFoldersConfigurable.invalidPathReason("/etc", null));
  }
}

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

import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The shared folder-path helpers: the duplicate rule the store applies, and the label both
 * custom-folder node kinds render while the provider sorts by the same string.
 */
public class FolderPathsTest extends GrailsTestCase {

  public void testNormalizeCollapsesTheCommonSpellings() {
    for (String spelling : Arrays.asList("grails-app/", "./grails-app", "grails-app\\", ".\\grails-app")) {
      assertEquals(spelling, "grails-app", FolderPaths.normalize(spelling));
    }
  }

  /**
   * A bare leading separator is not one of the spellings the javadoc lists — only a leading
   * {@code ./} or {@code .\} is stripped — so it survives normalization as a leading slash, which is
   * why {@code /grails-app} and {@code grails-app} are two different folders and not duplicates.
   */
  public void testNormalizeKeepsABareLeadingSeparator() {
    assertEquals("/grails-app", FolderPaths.normalize("\\grails-app"));
    assertEquals("/grails-app", FolderPaths.normalize("/grails-app"));
  }

  public void testDedupeTrimsDropsBlanksAndKeepsTheFirstSpelling() {
    assertEquals(Arrays.asList("a/", "b"),
                 FolderPaths.dedupe(Arrays.asList("  a/  ", "", "   ", "a", "a\\", "b")));
  }

  public void testDedupeKeepsEveryEntryWhenNothingCollapses() {
    assertEquals(Arrays.asList("a", "b", "c"),
                 FolderPaths.dedupe(Arrays.asList("a", "b", "c")));
  }

  public void testDedupeDoesNotTreatAnAbsolutePathAsASpellingOfARelativeOne() {
    assertEquals(Arrays.asList("a", "/a"),
                 FolderPaths.dedupe(Arrays.asList("a", "/a")));
  }

  public void testDedupeOfNothingIsEmpty() {
    assertTrue(FolderPaths.dedupe(Collections.<String>emptyList()).isEmpty());
    assertTrue(FolderPaths.dedupe(Arrays.asList(null, "  ")).isEmpty());
  }

  /**
   * The label is the last segment of the normalized path, so the sort the provider performs and the
   * label the nodes render cannot disagree about which folder a spelling names.
   */
  public void testFolderNameIsTheLastSegmentOfTheNormalizedPath() {
    assertEquals("docs", FolderPaths.folderName("docs"));
    assertEquals("docs", FolderPaths.folderName("docs/"));
    assertEquals("docs", FolderPaths.folderName("./docs/"));
    assertEquals("docs", FolderPaths.folderName("app\\docs\\"));
    assertEquals("c", FolderPaths.folderName("a/b/c"));
    assertEquals(".github", FolderPaths.folderName(".github"));
  }

  public void testFolderNameOfAnEmptyOrRootPathIsEmpty() {
    assertEquals("", FolderPaths.folderName(""));
    assertEquals("", FolderPaths.folderName("/"));
  }

  /**
   * Two folders can share a name at different depths, so the label alone does not identify them —
   * which is what the node's location string is for. What has to hold is that each label follows its
   * own path's last segment.
   */
  public void testFolderNameFollowsEachPathsOwnLastSegment() {
    List<String> names = Arrays.asList("docs", "app/docs", "a/b/c").stream()
      .map(FolderPaths::folderName).toList();
    assertEquals(Arrays.asList("docs", "docs", "c"), names);
  }
}

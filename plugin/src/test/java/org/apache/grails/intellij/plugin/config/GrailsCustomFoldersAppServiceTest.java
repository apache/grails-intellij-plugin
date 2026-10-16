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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


public class GrailsCustomFoldersAppServiceTest extends GrailsTestCase {

  public void testSetAndGetFolders() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();

    assertTrue(service.getFolders().isEmpty());

    service.setFolders(Arrays.asList("one", "two", "three"));
    assertEquals(Arrays.asList("one", "two", "three"), service.getFolders());

    service.setFolders(Collections.<String>emptyList());
    assertTrue(service.getFolders().isEmpty());
  }

  public void testBlankAndEmptyEntriesAreDropped() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();

    service.setFolders(Arrays.asList("one", "", "   ", "\t", "two"));
    assertEquals(Arrays.asList("one", "two"), service.getFolders());

    service.setFolders(Arrays.asList("", "  "));
    assertTrue(service.getFolders().isEmpty());
  }

  /**
   * The cap limits what the view renders, not what is stored: a stored folder must survive the cap
   * being lowered, otherwise lowering it would silently delete a configuration.
   */
  public void testFoldersAreNotTruncatedOnSave() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();

    service.setFolders(Arrays.asList("one", "two", "three", "four"));

    assertEquals(Arrays.asList("one", "two", "three", "four"), service.getFolders());

    service.setFolders(Arrays.asList("one", "two", "three"));
    assertEquals(Arrays.asList("one", "two", "three"), service.getFolders());
  }

  public void testMaxFoldersDefaultsToSix() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();

    assertEquals(6, service.getMaxFolders());

    GrailsCustomFoldersAppService.StateHolder state = service.getState();
    assertEquals(6, state.maxFolders);
  }

  /**
   * A state file written before the cap became a setting has no {@code maxFolders} field, which
   * deserializes to 0, and a hand-edited file can hold anything else.
   */
  public void testStoredMaxFoldersOutsideTheRangeClampsToTheDefault() {
    assertEquals(6, roundTripMaxFolders(0));
    assertEquals(6, roundTripMaxFolders(21));
    assertEquals(6, roundTripMaxFolders(-1));
  }

  public void testMaxFoldersRoundTrip() {
    assertEquals(1, roundTripMaxFolders(1));
    assertEquals(6, roundTripMaxFolders(6));
    assertEquals(20, roundTripMaxFolders(20));
  }

  public void testSetMaxFoldersClampsIntoTheRange() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();

    service.setMaxFolders(0);
    assertEquals(1, service.getMaxFolders());

    service.setMaxFolders(-5);
    assertEquals(1, service.getMaxFolders());

    service.setMaxFolders(99);
    assertEquals(20, service.getMaxFolders());

    service.setMaxFolders(12);
    assertEquals(12, service.getMaxFolders());
  }

  public void testPersistenceRoundTrip() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();
    service.setFolders(Arrays.asList("one", "two", "three"));

    GrailsCustomFoldersAppService.StateHolder state = service.getState();

    GrailsCustomFoldersAppService restored = new GrailsCustomFoldersAppService();
    restored.loadState(state);

    assertEquals(service.getFolders(), restored.getFolders());
  }

  public void testPersistenceRoundTripEmptyState() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();

    GrailsCustomFoldersAppService.StateHolder state = service.getState();

    GrailsCustomFoldersAppService restored = new GrailsCustomFoldersAppService();
    restored.loadState(state);

    assertTrue(restored.getFolders().isEmpty());
  }

  public void testGetFoldersReturnsUnmodifiableCopy() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();
    service.setFolders(Arrays.asList("one", "two"));

    List<String> folders = service.getFolders();
    try {
      folders.add("three");
    }
    catch (UnsupportedOperationException ignored) {
      // an unmodifiable list rejects the mutation
    }

    assertEquals(Arrays.asList("one", "two"), service.getFolders());
  }

  public void testDuplicatePathsAreDeduplicated() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();
    service.setFolders(Arrays.asList("grails-app", "grails-app/", "grails-app\\"));
    assertEquals(Arrays.asList("grails-app"), service.getFolders());
  }

  public void testDuplicatePathsWithDotSlashPrefix() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();
    service.setFolders(Arrays.asList("src/main", "./src/main", ".\\src/main"));
    assertEquals(Arrays.asList("src/main"), service.getFolders());
  }

  public void testFirstInstanceIsKept() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();
    service.setFolders(Arrays.asList("grails-app/", "grails-app", "other"));
    assertEquals(Arrays.asList("grails-app/", "other"), service.getFolders());
  }

  public void testAbsolutePathAndRelativePathAreNotDuplicates() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();
    service.setFolders(Arrays.asList("grails-app", "/grails-app"));
    assertEquals(Arrays.asList("grails-app", "/grails-app"), service.getFolders());
  }

  /**
   * The cap never truncated the store, so every unique folder is kept however many spellings of it
   * were typed: four here, not three.
   */
  public void testDeduplicationIsNotBoundedByTheCap() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();
    service.setFolders(Arrays.asList("a", "a/", "b", "b/", "c", "c/", "d", "d/"));
    assertEquals(Arrays.asList("a", "b", "c", "d"), service.getFolders());
  }

  /**
   * The global defaults are application-level state, so every Grails pane has to hear about them:
   * each open project holds its own application-bus subscription and refreshes its own pane.
   */
  public void testSetFoldersPublishesGlobalFoldersChanged() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();
    boolean[] published = {false};
    MessageBusConnection connection =
      ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable());
    connection.subscribe(GrailsCustomFoldersAppService.GlobalFoldersChangedListener.TOPIC,
                         () -> published[0] = true);

    service.setFolders(Arrays.asList("one"));

    assertTrue("changing the global defaults must publish the application-level topic", published[0]);
  }

  /** The cap is global state too, so changing it must reach every Grails pane as well. */
  public void testSetMaxFoldersPublishesGlobalFoldersChanged() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();
    boolean[] published = {false};
    MessageBusConnection connection =
      ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable());
    connection.subscribe(GrailsCustomFoldersAppService.GlobalFoldersChangedListener.TOPIC,
                         () -> published[0] = true);

    service.setMaxFolders(2);

    assertTrue("changing the cap must publish the application-level topic", published[0]);
  }

  /**
   * The settings page writes the folder list and the cap in one Apply, and a publish per field would
   * rebuild every open Grails pane once per field. Both fields are written here and one event goes
   * out, so a single publish carries both of them.
   */
  public void testSetGlobalDefaultsWritesBothFieldsAndPublishesOnce() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();
    int[] published = {0};
    MessageBusConnection connection =
      ApplicationManager.getApplication().getMessageBus().connect(getTestRootDisposable());
    connection.subscribe(GrailsCustomFoldersAppService.GlobalFoldersChangedListener.TOPIC,
                         () -> published[0]++);

    service.setGlobalDefaults(Arrays.asList("one", "two"), 12);

    assertEquals("the folder list and the cap are written together",
                 Arrays.asList("one", "two"), service.getFolders());
    assertEquals(12, service.getMaxFolders());
    assertEquals("one Apply must publish exactly one change", 1, published[0]);
  }

  /** The cap is still clamped through the combined write, so a bad row cannot corrupt the setting. */
  public void testSetGlobalDefaultsClampsTheCap() {
    GrailsCustomFoldersAppService service = new GrailsCustomFoldersAppService();

    service.setGlobalDefaults(Collections.<String>emptyList(), 0);
    assertEquals(GrailsCustomFoldersAppService.MIN_FOLDERS, service.getMaxFolders());

    service.setGlobalDefaults(Collections.<String>emptyList(), 99);
    assertEquals(GrailsCustomFoldersAppService.MAX_FOLDERS, service.getMaxFolders());
  }

  private static int roundTripMaxFolders(int stored) {
    GrailsCustomFoldersAppService.StateHolder state = new GrailsCustomFoldersAppService.StateHolder();
    state.maxFolders = stored;
    GrailsCustomFoldersAppService restored = new GrailsCustomFoldersAppService();
    restored.loadState(state);
    return restored.getMaxFolders();
  }
}

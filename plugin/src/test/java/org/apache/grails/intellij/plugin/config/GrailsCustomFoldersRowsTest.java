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

import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.ui.components.JBScrollPane;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.annotations.NotNull;

import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;


/**
 * The row panel behind the custom-folders settings page: rows are the unit of editing, a blank row
 * is not a folder, the panel scrolls, and no number of rows is capped.
 */
public class GrailsCustomFoldersRowsTest extends GrailsTestCase {

  // The rows are Swing components, which the minimal Mock JDK 11 does not ship.
  @Override
  protected @NotNull Supplier<Sdk> getTestJdk() {
    return () -> JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false);
  }

  public void testAppendedRowIsReadBack() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    rows.addRow("docs");
    assertEquals(Collections.singletonList("docs"), rows.getRows());
  }

  public void testRemovedRowIsGone() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    rows.addRow("first");
    rows.addRow("second");
    rows.removeRow(0);
    assertEquals(1, rows.getRowCount());
    assertEquals(Collections.singletonList("second"), rows.getRows());
  }

  public void testRemoveOutOfRangeIsANoOp() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    rows.addRow("first");
    rows.removeRow(5);
    rows.removeRow(-1);
    assertEquals(1, rows.getRowCount());
    assertEquals(Collections.singletonList("first"), rows.getRows());
  }

  /** A blank row is not a folder, but it stays on screen until the user removes it. */
  public void testBlankRowIsNotAFolder() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    rows.addRow("docs");
    rows.addRow("   ");
    rows.addRow("");
    assertEquals(3, rows.getRowCount());
    assertEquals(Collections.singletonList("docs"), rows.getRows());
  }

  public void testRowsAreReadBackInRowOrder() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    rows.addRow("grails-app");
    rows.addRow("docs");
    rows.addRow("src/integration-test");
    assertEquals(Arrays.asList("grails-app", "docs", "src/integration-test"), rows.getRows());
  }

  public void testComponentCarriesAScrollPane() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    assertTrue("the rows must be reachable by scrolling", hasScrollPane(rows.getComponent()));
  }

  /** S14: twenty rows survive the reader, none of them dropped for being over the cap. */
  public void testTwentyRowsAllSurviveTheReader() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    List<String> expected = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      String path = "folder" + i;
      expected.add(path);
      rows.addRow(path);
    }
    assertEquals(20, rows.getRowCount());
    assertEquals(expected, rows.getRows());
    assertTrue(hasScrollPane(rows.getComponent()));
  }

  /** A listener registered before any row exists hears the rows appended after it, and the edits. */
  public void testRowsChangedListenerHearsAppendedRowsAndEdits() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    int[] events = {0};
    rows.addRowsChangedListener(() -> events[0]++);

    rows.addRow("docs");
    assertEquals(1, events[0]);

    rows.addRow("grails-app");
    assertEquals(2, events[0]);

    List<JTextField> fields = textFieldsOf(rows.getComponent());
    assertEquals(2, fields.size());
    fields.get(0).setText("docs/");
    assertTrue("an edit to a row must reach the listener", events[0] > 2);
    assertEquals(Arrays.asList("docs/", "grails-app"), rows.getRows());
  }

  /** A registered listener hears a removal too, so the preview can drop the row that went away. */
  public void testRowsChangedListenerHearsARemoval() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    int[] events = {0};
    rows.addRowsChangedListener(() -> events[0]++);

    rows.addRow("docs");
    rows.addRow("grails-app");
    int appends = events[0];

    rows.removeRow(0);

    assertEquals("one event for the row removed, on top of the two appends",
                 appends + 1, events[0]);
    assertEquals(Collections.singletonList("grails-app"), rows.getRows());
  }

  /** Registering a second listener must not make one keystroke fire the first twice. */
  public void testTwoListenersDoNotDoubleCountOneEdit() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    int[] first = {0};
    int[] second = {0};
    rows.addRowsChangedListener(() -> first[0]++);
    rows.addRowsChangedListener(() -> second[0]++);

    rows.addRow("docs");
    textFieldsOf(rows.getComponent()).get(0).setText("docs/");

    assertEquals("each listener is called once per edit", first[0], second[0]);
    assertTrue("and an edit really did fire", first[0] > 1);
  }

  public void testSetRowsWithAnEmptyCollectionClearsTheRows() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    rows.addRow("docs");
    rows.addRow("grails-app");
    rows.setRows(Collections.<String>emptyList());
    assertEquals(0, rows.getRowCount());
    assertTrue(rows.getRows().isEmpty());
  }

  public void testSetRowsPreservesOrderAndTrims() {
    GrailsCustomFoldersRows rows = new GrailsCustomFoldersRows();
    rows.addRow("stale");
    rows.setRows(Arrays.asList("  grails-app ", "docs"));
    assertEquals(2, rows.getRowCount());
    assertEquals(Arrays.asList("grails-app", "docs"), rows.getRows());
  }

  private static @NotNull List<JTextField> textFieldsOf(@NotNull JComponent component) {
    List<JTextField> fields = new ArrayList<>();
    for (Component child : component.getComponents()) {
      if (child instanceof JTextField) {
        fields.add((JTextField)child);
      }
      else if (child instanceof JComponent) {
        fields.addAll(textFieldsOf((JComponent)child));
      }
    }
    return fields;
  }

  private static boolean hasScrollPane(@NotNull JComponent component) {
    for (Component child : component.getComponents()) {
      if (child instanceof JBScrollPane) return true;
      if (child instanceof JComponent && hasScrollPane((JComponent)child)) return true;
    }
    return false;
  }
}

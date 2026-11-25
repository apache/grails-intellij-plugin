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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.ui.components.JBScrollPane;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.JTextField;
import java.awt.Component;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

/**
 * One folder list on the custom-folders settings page: rows are the unit of editing, a blank row is
 * not a folder, the list scrolls, no number of rows is capped, the order is the user's, the chooser
 * stores a folder relative to the application root when it can, and each row says whether its path
 * resolves to a folder.
 */
public class GrailsCustomFoldersListTest extends GrailsTestCase {

  // The list is Swing, which the minimal Mock JDK 11 does not ship.
  @Override
  protected @NotNull Supplier<Sdk> getTestJdk() {
    return () -> JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false);
  }

  public void testAppendedRowIsReadBack() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("docs");
    assertEquals(Collections.singletonList("docs"), rows.getRows());
  }

  public void testRemovedRowIsGone() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("first");
    rows.addRow("second");
    rows.removeRow(0);
    assertEquals(1, rows.getRowCount());
    assertEquals(Collections.singletonList("second"), rows.getRows());
  }

  public void testRemoveOutOfRangeIsANoOp() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("first");
    rows.removeRow(5);
    rows.removeRow(-1);
    assertEquals(1, rows.getRowCount());
    assertEquals(Collections.singletonList("first"), rows.getRows());
  }

  /** A blank row is not a folder, but it stays in the list until the user removes it. */
  public void testBlankRowIsNotAFolder() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("docs");
    rows.addRow("   ");
    rows.addRow("");
    assertEquals(3, rows.getRowCount());
    assertEquals(Collections.singletonList("docs"), rows.getRows());
    assertEquals("the raw rows keep the blanks, so a problem can name the row the user sees",
                 Arrays.asList("docs", "", ""), rows.rawRows());
  }

  public void testRowsAreReadBackInRowOrder() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("grails-app");
    rows.addRow("docs");
    rows.addRow("src/integration-test");
    assertEquals(Arrays.asList("grails-app", "docs", "src/integration-test"), rows.getRows());
  }

  public void testComponentCarriesAScrollPane() {
    GrailsCustomFoldersList rows = list();
    assertNotNull("the rows must be reachable by scrolling", firstOfType(rows.getComponent(), JBScrollPane.class));
  }

  /** Twenty rows survive the reader, none of them dropped for being over the cap. */
  public void testTwentyRowsAllSurviveTheReader() {
    GrailsCustomFoldersList rows = list();
    List<String> expected = new ArrayList<>();
    for (int i = 0; i < 20; i++) {
      String path = "folder" + i;
      expected.add(path);
      rows.addRow(path);
    }
    assertEquals(20, rows.getRowCount());
    assertEquals(expected, rows.getRows());
  }

  /** A listener registered before any row exists hears the rows appended after it, and the edits. */
  public void testRowsChangedListenerHearsAppendedRowsAndEdits() {
    GrailsCustomFoldersList rows = list();
    int[] events = {0};
    rows.addRowsChangedListener(() -> events[0]++);

    rows.addRow("docs");
    assertEquals(1, events[0]);

    rows.addRow("grails-app");
    assertEquals(2, events[0]);

    table(rows).setValueAt("docs/", 0, 0);
    assertEquals("an edit to a row must reach the listener", 3, events[0]);
    assertEquals(Arrays.asList("docs/", "grails-app"), rows.getRows());
  }

  /** A registered listener hears a removal too, so the preview can drop the row that went away. */
  public void testRowsChangedListenerHearsARemoval() {
    GrailsCustomFoldersList rows = list();
    int[] events = {0};
    rows.addRowsChangedListener(() -> events[0]++);

    rows.addRow("docs");
    rows.addRow("grails-app");
    int appends = events[0];

    rows.removeRow(0);

    assertEquals("one event for the row removed, on top of the two appends", appends + 1, events[0]);
    assertEquals(Collections.singletonList("grails-app"), rows.getRows());
  }

  /** Registering a second listener must not make one edit fire the first twice. */
  public void testTwoListenersDoNotDoubleCountOneEdit() {
    GrailsCustomFoldersList rows = list();
    int[] first = {0};
    int[] second = {0};
    rows.addRowsChangedListener(() -> first[0]++);
    rows.addRowsChangedListener(() -> second[0]++);

    rows.addRow("docs");
    table(rows).setValueAt("docs/", 0, 0);

    assertEquals("each listener is called once per change", 2, first[0]);
    assertEquals(first[0], second[0]);
  }

  public void testSetRowsWithAnEmptyCollectionClearsTheRows() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("docs");
    rows.addRow("grails-app");
    rows.setRows(Collections.<String>emptyList());
    assertEquals(0, rows.getRowCount());
    assertTrue(rows.getRows().isEmpty());
  }

  public void testSetRowsPreservesOrderAndTrims() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("stale");
    rows.setRows(Arrays.asList("  grails-app ", "docs"));
    assertEquals(2, rows.getRowCount());
    assertEquals(Arrays.asList("grails-app", "docs"), rows.getRows());
  }

  /** The order of the rows is the order the Grails view renders them in, so the user can change it. */
  public void testMoveUpAndMoveDownReorderTheRows() {
    GrailsCustomFoldersList rows = list();
    rows.setRows(Arrays.asList("a", "b", "c"));
    int[] events = {0};
    rows.addRowsChangedListener(() -> events[0]++);

    rows.moveUp(2);
    assertEquals(Arrays.asList("a", "c", "b"), rows.getRows());
    assertTrue("a move reaches the listener, so the preview follows it", events[0] > 0);
    assertEquals("the moved row stays selected", 1, table(rows).getSelectedRow());

    rows.moveDown(0);
    assertEquals(Arrays.asList("c", "a", "b"), rows.getRows());
    assertEquals(1, table(rows).getSelectedRow());
  }

  public void testMovesPastEitherEndAreNoOps() {
    GrailsCustomFoldersList rows = list();
    rows.setRows(Arrays.asList("a", "b"));

    rows.moveUp(0);
    rows.moveDown(1);
    rows.moveUp(5);
    rows.moveDown(-1);

    assertEquals(Arrays.asList("a", "b"), rows.getRows());
  }

  /** A folder under the application root is stored relative to it, so the setting survives a move. */
  public void testChosenFolderUnderTheApplicationRootIsStoredRelative() throws IOException {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("app");
    VirtualFile docs = myFixture.getTempDirFixture().findOrCreateDir("app/src/docs");
    VirtualFile[] offered = {null};
    GrailsCustomFoldersList rows = new GrailsCustomFoldersList(() -> root, (parent, toSelect) -> {
      offered[0] = toSelect;
      return docs;
    });

    rows.addChosenFolder();

    assertEquals(Collections.singletonList("src/docs"), rows.getRows());
    assertEquals("the chooser opens at the application root", root, offered[0]);
    assertEquals("the added row is selected", 0, table(rows).getSelectedRow());
  }

  public void testChosenFolderOutsideTheApplicationRootIsNotStored() throws IOException {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("app");
    VirtualFile outside = OutsideProjectDirectory.create(getTestRootDisposable(), "list-chooser");
    GrailsCustomFoldersList rows = new GrailsCustomFoldersList(() -> root, (parent, toSelect) -> outside);

    rows.addChosenFolder();

    assertEquals(0, rows.getRowCount());
  }

  public void testChoosingAFolderAlreadyListedSelectsItInsteadOfAddingItTwice() throws IOException {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("app");
    VirtualFile docs = myFixture.getTempDirFixture().findOrCreateDir("app/docs");
    GrailsCustomFoldersList rows = new GrailsCustomFoldersList(() -> root, (parent, toSelect) -> docs);
    rows.setRows(Arrays.asList("docs/", "other"));
    table(rows).getSelectionModel().setSelectionInterval(1, 1);

    rows.addChosenFolder();

    assertEquals(Arrays.asList("docs/", "other"), rows.getRows());
    assertEquals("the existing spelling of the folder is selected", 0, table(rows).getSelectedRow());
  }

  public void testCancellingTheChooserOrChoosingTheApplicationRootAddsNothing() throws IOException {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("app");
    VirtualFile[] choice = {null};
    GrailsCustomFoldersList rows = new GrailsCustomFoldersList(() -> root, (parent, toSelect) -> choice[0]);

    rows.addChosenFolder();
    choice[0] = root;
    rows.addChosenFolder();

    assertEquals(0, rows.getRowCount());
  }

  /** A path the chooser cannot pick yet, such as a build output folder, is typed into a new row. */
  public void testTypedPathIsEditedInPlaceAndKept() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("docs");

    rows.addTypedPath();
    assertTrue("the new row is opened for editing", rows.isEditing(1));
    editor(rows).setText("build");
    rows.stopEditing();

    assertEquals(Arrays.asList("docs", "build"), rows.getRows());
  }

  public void testTypedPathLeftEmptyOrAbandonedIsDropped() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("docs");

    rows.addTypedPath();
    rows.cancelEditing();
    assertEquals("Escape leaves no blank row behind", 1, rows.getRowCount());

    rows.addTypedPath();
    editor(rows).setText("   ");
    rows.stopEditing();
    assertEquals("nor does confirming an empty row", 1, rows.getRowCount());
  }

  /** Each row says what its path resolves to, so a typo or a file is visible before Apply. */
  public void testRowStatusSaysWhetherThePathResolvesToAFolder() throws IOException {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("app");
    myFixture.getTempDirFixture().findOrCreateDir("app/docs");
    myFixture.addFileToProject("app/README.md", "# readme");
    GrailsCustomFoldersList rows = new GrailsCustomFoldersList(() -> root, (parent, toSelect) -> null);
    rows.setRows(Arrays.asList("docs", "not-created-yet", "README.md"));

    assertEquals(GrailsCustomFoldersList.Status.FOLDER, rows.statusAt(0));
    assertNull("a folder needs no note", rows.noteAt(0));
    assertEquals(GrailsCustomFoldersList.Status.MISSING, rows.statusAt(1));
    assertEquals("not found; shown as an empty folder until it exists", rows.noteAt(1));
    assertEquals(GrailsCustomFoldersList.Status.NOT_A_FOLDER, rows.statusAt(2));
    assertEquals("is a file, not a folder", rows.noteAt(2));
  }

  public void testEditingARowRereadsItsStatus() throws IOException {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("app");
    myFixture.getTempDirFixture().findOrCreateDir("app/docs");
    GrailsCustomFoldersList rows = new GrailsCustomFoldersList(() -> root, (parent, toSelect) -> null);
    rows.addRow("dosc");
    assertEquals(GrailsCustomFoldersList.Status.MISSING, rows.statusAt(0));

    table(rows).setValueAt("docs", 0, 0);

    assertEquals(GrailsCustomFoldersList.Status.FOLDER, rows.statusAt(0));
  }

  /**
   * A listener that reads the rows while an edit is being committed must not re-enter editingStopped:
   * getRows() stops editing only when no commit is in flight, so the stack cannot overflow. This is
   * the shape of the real settings page, where the preview calls getRows() on every change.
   */
  public void testListenerReadingRowsDuringCommitDoesNotOverflow() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("docs");

    int[] events = {0};
    rows.addRowsChangedListener(() -> {
      events[0]++;
      // Reading the rows is what the settings page does in its preview; doing it here, during the
      // commit, is what used to overflow the stack.
      rows.getRows();
      rows.rawRows();
    });

    rows.addTypedPath();
    assertTrue("the new row is opened for editing", rows.isEditing(1));
    editor(rows).setText("build");
    rows.stopEditing();

    assertEquals(Arrays.asList("docs", "build"), rows.getRows());
    assertTrue("the commit must reach the listener", events[0] > 0);
  }

  /** Without an application root only an absolute path can be checked. */
  public void testRelativeRowIsUncheckedWithoutAnApplicationRoot() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("docs");

    assertEquals(GrailsCustomFoldersList.Status.UNKNOWN, rows.statusAt(0));
    assertNull(rows.noteAt(0));
  }

  /** The path cell editor needs a double-click, so a single click selects the row instead of editing. */
  public void testPathCellEditorRequiresTwoClicksToStartEditing() {
    GrailsCustomFoldersList rows = list();
    rows.addRow("docs");
    JTable table = table(rows);
    javax.swing.table.TableCellEditor editor = table.getCellEditor(0, 0);
    assertTrue("the column supplies its own editor", editor instanceof javax.swing.DefaultCellEditor);
    assertEquals(2, ((javax.swing.DefaultCellEditor) editor).getClickCountToStart());
  }

  private static @NotNull GrailsCustomFoldersList list() {
    return new GrailsCustomFoldersList(() -> null, (parent, toSelect) -> null);
  }

  private static @NotNull JTable table(@NotNull GrailsCustomFoldersList rows) {
    JTable table = firstOfType(rows.getComponent(), JTable.class);
    assertNotNull("the list is a table", table);
    return table;
  }

  private static @NotNull JTextField editor(@NotNull GrailsCustomFoldersList rows) {
    Component editor = table(rows).getEditorComponent();
    assertTrue("a row is being edited in a text field: " + editor, editor instanceof JTextField);
    return (JTextField)editor;
  }

  private static <T> @Nullable T firstOfType(@NotNull Component root, @NotNull Class<T> type) {
    if (type.isInstance(root)) return type.cast(root);
    if (root instanceof JComponent component) {
      for (Component child : component.getComponents()) {
        T found = firstOfType(child, type);
        if (found != null) return found;
      }
    }
    return null;
  }
}

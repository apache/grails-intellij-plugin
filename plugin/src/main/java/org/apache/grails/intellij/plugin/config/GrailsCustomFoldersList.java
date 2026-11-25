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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbAwareAction;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.GuiUtils;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.TableUtil;
import com.intellij.ui.ToolbarDecorator;
import com.intellij.ui.table.TableView;
import com.intellij.util.ui.ColumnInfo;
import com.intellij.util.ui.ListTableModel;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ChangeEvent;
import javax.swing.table.TableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

/**
 * One ordered list of folder paths on the custom-folders settings page. Folders are added with the
 * platform's folder chooser or typed in, edited in place, removed and moved up or down; the order is
 * the order the Grails view renders them in. Each row says whether its path resolves to a folder.
 */
public class GrailsCustomFoldersList extends JPanel {

  /** Picks a directory for the add button. The settings page opens the platform's folder chooser. */
  public interface FolderChooser {
    @Nullable VirtualFile choose(@NotNull JComponent parent, @Nullable VirtualFile toSelect);
  }

  /** What a row's path currently resolves to. */
  enum Status {
    FOLDER,
    MISSING,
    NOT_A_FOLDER,
    UNKNOWN
  }

  private static final class Row {
    private @NotNull String path;
    private @NotNull Status status;

    private Row(@NotNull String path, @NotNull Status status) {
      this.path = path;
      this.status = status;
    }
  }

  private final @NotNull Supplier<@Nullable VirtualFile> applicationRoot;
  private final @NotNull FolderChooser chooser;
  private final @NotNull ListTableModel<Row> model;
  private final @NotNull TableView<Row> table;
  private final @NotNull List<Runnable> listeners = new ArrayList<>();

  // Depth of in-flight cell commits. setValueAt fires the model listener, which can call back into
  // getRows()/rawRows() (and from there stopEditing) before the commit has finished. Guarding
  // stopEditing while this is non-zero breaks the re-entrant setValueAt loop that otherwise
  // overflows the stack (see issue #470). A counter rather than a boolean keeps the guard correct
  // if a commit ever triggers another commit before the outer one finishes.
  private int committingEditDepth = 0;

  /**
   * @param applicationRoot the root relative paths are resolved against, for the row status and for
   *                        storing a chosen folder relative to it; null when it is not known
   */
  public GrailsCustomFoldersList(@NotNull Supplier<@Nullable VirtualFile> applicationRoot, @NotNull FolderChooser chooser) {
    super(new BorderLayout());
    this.applicationRoot = applicationRoot;
    this.chooser = chooser;
    this.model = new ListTableModel<>(new PathColumn());
    this.table = new TableView<>(model) {
      // A row typed in and then left empty, or abandoned with Escape, is not a folder: drop it rather
      // than leave a blank line behind.
      @Override
      public void editingStopped(ChangeEvent e) {
        int row = getEditingRow();
        committingEditDepth++;
        try {
          super.editingStopped(e);
        } finally {
          committingEditDepth--;
        }
        dropIfBlank(row);
      }

      @Override
      public void editingCanceled(ChangeEvent e) {
        int row = getEditingRow();
        committingEditDepth++;
        try {
          super.editingCanceled(e);
        } finally {
          committingEditDepth--;
        }
        dropIfBlank(row);
      }
    };
    table.setTableHeader(null);
    table.setShowGrid(false);
    table.setVisibleRowCount(4);
    table.putClientProperty("terminateEditOnFocusLost", Boolean.TRUE);
    table.getEmptyText().setText(GrailsBundle.message("grails.settings.custom.folders.empty"));
    model.addTableModelListener(event -> notifyListeners());

    JPanel decorated = ToolbarDecorator.createDecorator(table, null)
      .setAddAction(button -> addChosenFolder())
      .setAddActionName(GrailsBundle.message("grails.settings.custom.folders.add.folder"))
      .addExtraAction(new DumbAwareAction(GrailsBundle.message("grails.settings.custom.folders.add.path"), null,
                                          AllIcons.Actions.Edit) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          addTypedPath();
        }
      })
      .setRemoveAction(button -> removeRow(table.getSelectedRow()))
      .setMoveUpAction(button -> moveUp(table.getSelectedRow()))
      .setMoveDownAction(button -> moveDown(table.getSelectedRow()))
      .createPanel();
    add(decorated, BorderLayout.CENTER);
  }

  /** The component to hand to the settings form; it already carries its toolbar and scroll pane. */
  public @NotNull JComponent getComponent() {
    return this;
  }

  /** The trimmed non-empty paths, in row order. Nothing is capped here: the cap belongs to the view. */
  public @NotNull List<String> getRows() {
    if (committingEditDepth == 0) TableUtil.stopEditing(table);
    List<String> result = new ArrayList<>();
    for (Row row : model.getItems()) {
      String path = row.path.trim();
      if (!path.isEmpty()) result.add(path);
    }
    return result;
  }

  /**
   * Every row's path as it stands, in row order, blanks included: what the user sees. A problem
   * reported against one row must name the number beside that row.
   */
  public @NotNull List<String> rawRows() {
    if (committingEditDepth == 0) TableUtil.stopEditing(table);
    List<String> result = new ArrayList<>();
    for (Row row : model.getItems()) {
      result.add(row.path);
    }
    return result;
  }

  public int getRowCount() {
    return model.getRowCount();
  }

  /** Replaces every row with the given paths, in order. */
  public void setRows(@NotNull Collection<String> paths) {
    if (committingEditDepth == 0) TableUtil.stopEditing(table);
    List<Row> rows = new ArrayList<>();
    for (String path : paths) {
      rows.add(row(path == null ? "" : path.trim()));
    }
    model.setItems(rows);
  }

  /** Appends a row holding the given path. */
  public void addRow(@NotNull String path) {
    model.addRow(row(path.trim()));
  }

  /** Removes the row at the given index, if there is one, keeping a neighbour selected. */
  public void removeRow(int index) {
    if (index < 0 || index >= model.getRowCount()) return;
    if (committingEditDepth == 0) TableUtil.stopEditing(table);
    model.removeRow(index);
    select(Math.min(index, model.getRowCount() - 1));
  }

  /** Moves the row at the given index one place up, so the Grails view renders it earlier. */
  void moveUp(int index) {
    if (index <= 0 || index >= model.getRowCount()) return;
    if (committingEditDepth == 0) TableUtil.stopEditing(table);
    model.exchangeRows(index - 1, index);
    select(index - 1);
  }

  /** Moves the row at the given index one place down, so the Grails view renders it later. */
  void moveDown(int index) {
    if (index < 0 || index >= model.getRowCount() - 1) return;
    if (committingEditDepth == 0) TableUtil.stopEditing(table);
    model.exchangeRows(index, index + 1);
    select(index + 1);
  }

  /**
   * The add button: asks the chooser for a folder and appends it, relative to the application root
   * when it sits under it and absolute otherwise. A folder already in the list is selected instead
   * of added twice.
   */
  void addChosenFolder() {
    VirtualFile root = applicationRoot.get();
    VirtualFile chosen = chooser.choose(this, root);
    if (chosen == null) return;
    String path = FolderPaths.toConfiguredPath(root, chosen);
    if (path == null) return;
    // Only relative paths are supported: an absolute location cannot be read across projects.
    if (FolderPaths.isAbsolute(path)) return;
    String normalized = FolderPaths.normalize(path);
    for (int i = 0; i < model.getRowCount(); i++) {
      if (FolderPaths.normalize(model.getItem(i).path).equals(normalized)) {
        select(i);
        return;
      }
    }
    addRow(path);
    select(model.getRowCount() - 1);
  }

  /** Appends an empty row and starts editing it, for a path the chooser cannot pick yet. */
  void addTypedPath() {
    if (committingEditDepth == 0) TableUtil.stopEditing(table);
    model.addRow(row(""));
    int index = model.getRowCount() - 1;
    select(index);
    if (table.editCellAt(index, 0)) {
      Component editor = table.getEditorComponent();
      if (editor != null) editor.requestFocusInWindow();
    }
  }

  /** Whether the row at the given index is being edited. */
  boolean isEditing(int index) {
    return table.isEditing() && table.getEditingRow() == index;
  }

  /** Whether any row is currently being edited. */
  boolean isEditingAny() {
    return table.isEditing();
  }

  /** Ends any edit in progress, keeping the typed text; a row left empty is dropped. */
  void stopEditing() {
    if (committingEditDepth == 0) TableUtil.stopEditing(table);
  }

  /** Ends any edit in progress, discarding the typed text; a row left empty is dropped. */
  void cancelEditing() {
    // Do not cancel an editor while a commit is already in flight: it would call cancelCellEditing
    // on an editor that super.editingStopped has not yet removed.
    if (committingEditDepth > 0) return;
    if (table.isEditing()) table.getCellEditor().cancelCellEditing();
  }

  /** Notified on every append, edit, removal and move, now and later. */
  public void addRowsChangedListener(@NotNull Runnable listener) {
    listeners.add(listener);
  }

  /** What the row's path resolves to, as the row shows it. */
  @NotNull Status statusAt(int index) {
    return model.getItem(index).status;
  }

  /** The note the row shows beside its path, or null when the path resolves to a folder. */
  @Nullable String noteAt(int index) {
    return switch (model.getItem(index).status) {
      case MISSING -> GrailsBundle.message("grails.settings.custom.folders.status.missing");
      case NOT_A_FOLDER -> GrailsBundle.message("grails.settings.custom.folders.not.a.folder");
      case FOLDER, UNKNOWN -> null;
    };
  }

  /**
   * What the path resolves to: a folder, nothing yet (the view shows a placeholder), or a file. A
   * relative path with no known root cannot be checked.
   */
  static @NotNull Status statusOf(@Nullable VirtualFile root, @NotNull String path) {
    if (path.trim().isEmpty()) return Status.UNKNOWN;
    if (root == null && !FolderPaths.isAbsolute(path)) return Status.UNKNOWN;
    VirtualFile file = ReadAction.computeBlocking(() -> FolderPaths.find(root, path));
    if (file == null) return Status.MISSING;
    return file.isDirectory() ? Status.FOLDER : Status.NOT_A_FOLDER;
  }

  private @NotNull Row row(@NotNull String path) {
    return new Row(path, statusOf(applicationRoot.get(), path));
  }

  private void dropIfBlank(int index) {
    if (index >= 0 && index < model.getRowCount() && model.getItem(index).path.trim().isEmpty()) {
      model.removeRow(index);
    }
  }

  private void select(int index) {
    if (index >= 0 && index < model.getRowCount()) {
      table.getSelectionModel().setSelectionInterval(index, index);
    }
    else {
      table.clearSelection();
    }
  }

  private void notifyListeners() {
    for (Runnable listener : listeners) {
      listener.run();
    }
  }

  private final class PathColumn extends ColumnInfo<Row, String> {

    private final TableCellRenderer renderer = new ColoredTableCellRenderer() {
      @Override
      protected void customizeCellRenderer(@NotNull JTable table, @Nullable Object value, boolean selected,
                                           boolean hasFocus, int index, int column) {
        Row row = model.getItem(index);
        append(row.path);
        setIcon(switch (row.status) {
          case MISSING -> AllIcons.General.Warning;
          case NOT_A_FOLDER -> AllIcons.General.Error;
          case FOLDER, UNKNOWN -> AllIcons.Nodes.Folder;
        });
        String note = noteAt(index);
        if (note != null) {
          append("  " + note, row.status == Status.NOT_A_FOLDER
                              ? SimpleTextAttributes.ERROR_ATTRIBUTES
                              : SimpleTextAttributes.GRAYED_ATTRIBUTES);
        }
      }
    };

    private PathColumn() {
      super(GrailsBundle.message("grails.settings.custom.folders.column.folder"));
    }

    @Override
    public @Nullable String valueOf(Row row) {
      return row.path;
    }

    @Override
    public boolean isCellEditable(Row row) {
      return true;
    }

    @Override
    public @Nullable javax.swing.table.TableCellEditor getEditor(Row row) {
      // TableView's default String editor starts editing on a single click (clickCountToStart = 1),
      // so selecting a row to remove or move it instead drops straight into edit mode. Use an
      // editor that needs a double-click, like the platform's other editable tables, so a click
      // selects the row and editing stays an explicit gesture.
      javax.swing.DefaultCellEditor editor =
        new javax.swing.DefaultCellEditor(GuiUtils.createUndoableTextField());
      editor.setClickCountToStart(2);
      return editor;
    }

    @Override
    public void setValue(Row row, String value) {
      row.path = value == null ? "" : value.trim();
      row.status = statusOf(applicationRoot.get(), row.path);
    }

    @Override
    public @NotNull TableCellRenderer getRenderer(Row row) {
      return renderer;
    }
  }
}

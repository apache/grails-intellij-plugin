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

import com.intellij.ui.components.JBScrollPane;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.Scrollable;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A scrollable panel of single-line folder rows for the custom-folders settings page: {@code +}
 * appends a row, every row has its own remove button, and reading the panel returns the trimmed
 * non-empty rows in row order, capped by nothing, so any number of folders survives.
 */
public class GrailsCustomFoldersRows extends JPanel {

  private final @NotNull RowsPanel rowsPanel = new RowsPanel();
  private final @NotNull List<JTextField> fields = new ArrayList<>();
  private final @NotNull List<Runnable> listeners = new ArrayList<>();

  /**
   * One bridge shared by every row: any document event on any row means the folder list changed.
   * Attached once per field rather than once per listener, so registering a second listener cannot
   * make a single keystroke fire twice.
   */
  private final @NotNull DocumentListener rowsChanged = new DocumentListener() {

    @Override
    public void insertUpdate(DocumentEvent e) {
      notifyListeners();
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
      notifyListeners();
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
      notifyListeners();
    }
  };

  public GrailsCustomFoldersRows() {
    super(new BorderLayout());

    JButton addButton = new JButton("+");
    addButton.setToolTipText(GrailsBundle.message("grails.settings.custom.folders.row.add"));
    addButton.addActionListener(event -> addRow());

    JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
    toolbar.add(addButton);

    add(toolbar, BorderLayout.NORTH);
    add(new JBScrollPane(rowsPanel), BorderLayout.CENTER);
  }

  /** The component to hand to the settings form; it already carries its own scroll pane. */
  public @NotNull JComponent getComponent() {
    return this;
  }

  /** Appends an empty row. */
  public void addRow() {
    addRow("");
  }

  /** Appends a row holding the given path. */
  public void addRow(@NotNull String path) {
    JTextField field = new JTextField(path);
    JButton removeButton = new JButton("-");
    removeButton.setToolTipText(GrailsBundle.message("grails.settings.custom.folders.row.remove"));
    removeButton.addActionListener(event -> removeRow(fields.indexOf(field)));

    JPanel row = new JPanel(new BorderLayout());
    row.add(field, BorderLayout.CENTER);
    row.add(removeButton, BorderLayout.EAST);

    fields.add(field);
    field.getDocument().addDocumentListener(rowsChanged);
    rowsPanel.add(row);
    rowsPanel.revalidate();
    rowsPanel.repaint();
    notifyListeners();
  }

  /** Removes the row at the given index, if there is one. */
  public void removeRow(int index) {
    if (index < 0 || index >= fields.size()) return;
    fields.remove(index);
    rowsPanel.remove(index);
    rowsPanel.revalidate();
    rowsPanel.repaint();
    notifyListeners();
  }

  /** The number of rows, empty ones included. */
  public int getRowCount() {
    return fields.size();
  }

  /** The trimmed non-empty rows, in row order, with no row dropped for being over a cap. */
  public @NotNull List<String> getRows() {
    List<String> result = new ArrayList<>();
    for (JTextField field : fields) {
      String path = field.getText().trim();
      if (!path.isEmpty()) result.add(path);
    }
    return result;
  }

  /**
   * Every row's text as typed, in row order, blanks included: what the user sees. A problem reported
   * against one row must name the number beside that field, not the position the trimmed reader
   * happens to find it in.
   */
  public @NotNull List<String> rawRows() {
    List<String> result = new ArrayList<>();
    for (JTextField field : fields) {
      result.add(field.getText());
    }
    return result;
  }

  /** Replaces every row with the given paths, in order. */
  public void setRows(@NotNull Collection<String> rows) {
    fields.clear();
    rowsPanel.removeAll();
    for (String row : rows) {
      addRow(row == null ? "" : row);
    }
    rowsPanel.revalidate();
    rowsPanel.repaint();
  }

  /** Notified on every row edit, append and removal, now and later. */
  public void addRowsChangedListener(@NotNull Runnable listener) {
    listeners.add(listener);
  }

  private void notifyListeners() {
    for (Runnable listener : listeners) {
      listener.run();
    }
  }

  /** A vertical stack of rows that fills the viewport width and scrolls vertically. */
  private static final class RowsPanel extends JPanel implements Scrollable {

    RowsPanel() {
      setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
    }

    @Override
    public @NotNull Dimension getPreferredScrollableViewportSize() {
      return getPreferredSize();
    }

    @Override
    public int getScrollableUnitIncrement(@NotNull Rectangle visible, int orientation, int direction) {
      return 20;
    }

    @Override
    public int getScrollableBlockIncrement(@NotNull Rectangle visible, int orientation, int direction) {
      return visible.height;
    }

    @Override
    public boolean getScrollableTracksViewportWidth() {
      return true;
    }

    @Override
    public boolean getScrollableTracksViewportHeight() {
      return false;
    }
  }
}

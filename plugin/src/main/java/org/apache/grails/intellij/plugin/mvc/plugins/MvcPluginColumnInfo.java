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

package org.apache.grails.intellij.plugin.mvc.plugins;


import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NlsContexts.ColumnName;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.ColoredTableCellRenderer;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.util.ui.ColumnInfo;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;

import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.util.Comparator;

public abstract class MvcPluginColumnInfo extends ColumnInfo<MvcPluginDescriptor, String> {

  private final AvailablePluginsModel myModel;

  private final int myWidth;

  public MvcPluginColumnInfo(AvailablePluginsModel model, @ColumnName String title, int width) {
    super(title);
    myModel = model;
    myWidth = width;
  }

  @Override
  public abstract String valueOf(MvcPluginDescriptor mvcPlugin);

  @Override
  public Comparator<MvcPluginDescriptor> getComparator() {
    return (o1, o2) -> {
      int result = StringUtil.compare(valueOf(o1), valueOf(o2), true);
      if (result != 0) return result;

      return o1.getName().compareToIgnoreCase(o2.getName());
    };
  }

  @Override
  public TableCellRenderer getRenderer(MvcPluginDescriptor o) {
    return new MvcPluginCellRenderer(myModel);
  }

  @Override
  public Class getColumnClass() {
    //For all columns class is 'String'
    return String.class;
  }

  private static final class MvcPluginCellRenderer extends ColoredTableCellRenderer {
    private final AvailablePluginsModel myModel;

    private MvcPluginCellRenderer(final AvailablePluginsModel model) {
      myModel = model;
    }

    @Override
    protected void customizeCellRenderer(final @NotNull JTable table,
                                         final Object value,
                                         final boolean selected,
                                         final boolean hasFocus,
                                         final int row,
                                         final int column) {
      MvcPluginDescriptor mvcPlugin = ((MvcPluginsTable)table).getPluginAt(row);

      if (column == AvailablePluginsModel.COLUMN_NAME) {
        setIcon(AllIcons.Nodes.Pluginnotinstalled);

        appendRenderedText(mvcPlugin, mvcPlugin.getName());
      }
      else if (column == AvailablePluginsModel.COLUMN_TITLE) {
        appendRenderedText(mvcPlugin, mvcPlugin.getTitle());
      }
      else if (column == AvailablePluginsModel.COLUMN_VERSION) {
        appendRenderedText(mvcPlugin, mvcPlugin.getLatestVersion());
      }
    }

    private void appendRenderedText(MvcPluginDescriptor mvcPlugin, @Nls String text) {
      if (text == null) text = "";

      final MvcPluginIsInstalledColumnInfo isInstalledColumnInfo =
        (MvcPluginIsInstalledColumnInfo)myModel.getColumnInfos()[AvailablePluginsModel.COLUMN_IS_INSTALLED];

      if (isInstalledColumnInfo.getToRemovePlugins().contains(mvcPlugin.getName())) {
        SimpleTextAttributes deleteSimpleTextAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, MvcPluginUtil.COLOR_REMOVE_PLUGIN.get());
        append(text, deleteSimpleTextAttributes);
      }
      else if (isInstalledColumnInfo.getToInstallPlugins().contains(mvcPlugin.getName())) {
        SimpleTextAttributes installSimpleTextAttributes = new SimpleTextAttributes(SimpleTextAttributes.STYLE_PLAIN, MvcPluginUtil.COLOR_INSTALL_PLUGIN.get());
        append(text, installSimpleTextAttributes);
      }
      else {
        append(text, SimpleTextAttributes.SIMPLE_CELL_ATTRIBUTES);
      }
    }
  }

  @Override
  public int getWidth(JTable table) {
    return myWidth;
  }
}

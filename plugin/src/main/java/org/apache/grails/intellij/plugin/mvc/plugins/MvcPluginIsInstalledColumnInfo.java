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

import com.intellij.ui.BooleanTableCellEditor;
import com.intellij.ui.BooleanTableCellRenderer;
import com.intellij.util.ui.ColumnInfo;
import org.apache.grails.intellij.plugin.GrailsBundle;

import javax.swing.JTable;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Set;

public class MvcPluginIsInstalledColumnInfo extends ColumnInfo<MvcPluginDescriptor, Boolean> {

  private final Set<String> toInstallPlugins = new HashSet<>();
  private final Set<String> toRemovePlugins = new HashSet<>();
  private final Set<String> myInstalledPlugins;

  public MvcPluginIsInstalledColumnInfo(final Set<String> installedPlugins) {
    super(GrailsBundle.message("mvc.plugins.column.name.enable"));
    myInstalledPlugins = installedPlugins;
  }

  public boolean isPluginSelectAsInstalled(MvcPluginDescriptor mvcPlugin) {
    if (toInstallPlugins.contains(mvcPlugin.getName())) {
      return true;
    }
    if (toRemovePlugins.contains(mvcPlugin.getName())) {
      return false;
    }

    return myInstalledPlugins.contains(mvcPlugin.getName());
  }

  @Override
  public Boolean valueOf(MvcPluginDescriptor mvcPlugin) {
    return isPluginSelectAsInstalled(mvcPlugin);
  }

  @Override
  public boolean isCellEditable(final MvcPluginDescriptor mvcPlugin) {
    return true;
  }

  @Override
  public Class getColumnClass() {
    return Boolean.class;
  }

  @Override
  public TableCellEditor getEditor(final MvcPluginDescriptor mvcPlugin) {
    return new BooleanTableCellEditor();
  }

  @Override
  public TableCellRenderer getRenderer(final MvcPluginDescriptor mvcPlugin) {
    return new BooleanTableCellRenderer();
  }

  @Override
  public void setValue(final MvcPluginDescriptor mvcPlugin, final Boolean value) {
    final String name = mvcPlugin.getName();

    if (value.booleanValue()) {
      if (!myInstalledPlugins.contains(name)) {
        toInstallPlugins.add(name);
      }

      toRemovePlugins.remove(name);
    }
    else {
      if (myInstalledPlugins.contains(name)) {
        toRemovePlugins.add(name);
      }

      toInstallPlugins.remove(name);
    }
  }

  @Override
  public Comparator<MvcPluginDescriptor> getComparator() {
    return (mvcPlugin1, mvcPlugin2) -> {
      boolean select1 = isPluginSelectAsInstalled(mvcPlugin1);
      boolean select2 = isPluginSelectAsInstalled(mvcPlugin2);
      if (select1 == select2) return mvcPlugin1.getName().compareToIgnoreCase(mvcPlugin2.getName());

      return select1 ? -1 : 1;
    };
  }

  @Override
  public int getWidth(final JTable table) {
    return 10;
  }

  public Set<String> getToInstallPlugins() {
    return toInstallPlugins;
  }

  public Set<String> getToRemovePlugins() {
    return toRemovePlugins;
  }
}

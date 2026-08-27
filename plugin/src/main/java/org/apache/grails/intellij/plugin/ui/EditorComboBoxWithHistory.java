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

package org.apache.grails.intellij.plugin.ui;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.fileTypes.PlainTextFileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.ComboBox;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.EditorTextField;
import com.intellij.ui.StringComboboxEditor;
import org.jetbrains.annotations.NotNull;

import javax.swing.AbstractListModel;
import javax.swing.ComboBoxModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class EditorComboBoxWithHistory extends ComboBox {

  private final @NotNull String myDataKey;

  public EditorComboBoxWithHistory(@NotNull Project project, @NotNull String key) {
    super(createModel(key));
    setEditor(new StringComboboxEditor(project, PlainTextFileType.INSTANCE, this));
    myDataKey = key;
  }

  public @NotNull EditorTextField getEditorComponent() {
    return (EditorTextField)super.getEditor().getEditorComponent();
  }

  public @NotNull String getText() {
    return getEditorComponent().getText();
  }

  @Override
  public MyModel getModel() {
    return (MyModel)super.getModel();
  }

  public void save() {
    String text = getText();
    List<String> strings = new ArrayList<>(getModel().myStrings);
    int index = strings.indexOf(text);
    if (index > -1) {
      strings.remove(index);
    }
    strings.add(0, text);
    PropertiesComponent.getInstance().setValue(myDataKey, StringUtil.join(strings, "\n"));
  }

  public static class MyModel extends AbstractListModel<String> implements ComboBoxModel<String> {

    private final List<@NlsSafe String> myStrings = new ArrayList<>();
    private @NlsSafe String myCurrentString;

    public MyModel() {
    }

    public MyModel(Collection<String> strings) {
      myStrings.addAll(strings);
    }

    @Override
    public void setSelectedItem(Object anItem) {
      int oldIndex = myStrings.indexOf(myCurrentString);
      myCurrentString = String.valueOf(anItem);
      int newIndex = myStrings.indexOf(myCurrentString);
      fireContentsChanged(anItem, oldIndex, newIndex);
    }

    @Override
    public Object getSelectedItem() {
      return myCurrentString;
    }

    @Override
    public int getSize() {
      return myStrings.size();
    }

    @Override
    public String getElementAt(int index) {
      return myStrings.get(index);
    }
  }

  private static @NotNull MyModel createModel(@NotNull String dataKey) {
    String history = PropertiesComponent.getInstance().getValue(dataKey);
    return history == null
           ? new MyModel()
           : new MyModel(Arrays.stream(history.split("\n")).filter(StringUtil::isNotEmpty).collect(Collectors.toList()));
  }
}

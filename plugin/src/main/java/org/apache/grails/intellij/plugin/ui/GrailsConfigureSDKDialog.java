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

import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.runner.ui.GrailsApplicationCombobox;
import org.apache.grails.intellij.plugin.sdk.GrailsSDKManager;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.apache.grails.intellij.plugin.structure.OldGrailsApplication;
import org.apache.grails.intellij.plugin.util.version.Range;
import org.apache.grails.intellij.plugin.util.version.Version;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class GrailsConfigureSDKDialog extends DialogWrapper {

  private final GrailsSDKHomeForm mySDKHomeForm;

  private JPanel myCenterPanel;
  private GrailsApplicationCombobox myApplicationCombobox;
  private JPanel mySDKFormPanel;
  private JLabel myApplicationLabel;

  public GrailsConfigureSDKDialog(@NotNull Project project) {
    super(project, false, IdeModalityType.IDE);

    mySDKHomeForm = new GrailsSDKHomeForm();
    mySDKHomeForm.setChangedCallback(this::checkOkAction);
    mySDKFormPanel.add(mySDKHomeForm.getComponent(), BorderLayout.CENTER);

    myApplicationLabel.setLabelFor(myApplicationCombobox);
    myApplicationCombobox.disallowEmptySelection();
    myApplicationCombobox.setApplications(GrailsApplicationManager.getInstance(project).getApplications());
    myApplicationCombobox.addItemListener(e -> {
      GrailsApplication selectedApplication = myApplicationCombobox.getSelectedApplication();
      mySDKHomeForm.setVersionRange(getVersionRange(selectedApplication));
      mySDKHomeForm.setPath(GrailsSDKManager.getGrailsSdkPath(selectedApplication));
    });
    myApplicationCombobox.addItemListener(e -> checkOkAction());

    setTitle(GrailsBundle.message("action.Grails.ChangeSDK.text"));
    init();
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return myCenterPanel;
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return mySDKHomeForm.getPathComponent();
  }

  private void checkOkAction() {
    setOKActionEnabled(myApplicationCombobox.getSelectedApplication() != null && mySDKHomeForm.validate());
  }

  public @NotNull GrailsConfigureSDKDialog setGrailsApplication(@Nullable GrailsApplication application) {
    myApplicationCombobox.setSelectedApplication(application);
    return this;
  }

  @Override
  protected void doOKAction() {
    final GrailsApplication application = myApplicationCombobox.getSelectedApplication();
    assert application != null;
    GrailsSDKManager.setGrailsSDK(application, mySDKHomeForm.getSelectedSdk().getPath());
    super.doOKAction();
  }

  private static @Nullable Range<Version> getVersionRange(@Nullable GrailsApplication application) {
    if (application == null) return null;
    if (application instanceof OldGrailsApplication) {
      return Version.LESS_THAN_3;
    }
    else if (application instanceof Grails3Application) {
      return Version.AT_LEAST_3;
    }
    else {
      return null;
    }
  }
}

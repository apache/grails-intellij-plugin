/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.grails.intellij.plugin.ui;

import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.editor.event.DocumentEvent;
import com.intellij.openapi.editor.event.DocumentListener;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.TextFieldCompletionProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.commands.GrailsCommandCompletionUtil;
import org.apache.grails.intellij.plugin.runner.GrailsCommandExecutor;
import org.apache.grails.intellij.plugin.runner.ui.GrailsApplicationCombobox;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.util.Objects;

public class GrailsRunCommandDialog extends DialogWrapper {

  private JPanel myMainPanel;

  private JBLabel myApplicationsLabel;
  private GrailsApplicationCombobox myApplications;

  private JBLabel myCommandLabel;
  private EditorComboBoxWithHistory myCommand;

  private JBLabel myVMOptionsLabel;
  private EditorComboBoxWithHistory myVMOptions;

  private final Project myProject;

  public GrailsRunCommandDialog(@NotNull Project project) {
    super(project, false, IdeModalityType.IDE);
    myProject = project;

    myApplicationsLabel.setLabelFor(myApplications);
    myApplications.disallowEmptySelection();
    myApplications.setApplications(GrailsApplicationManager.getInstance(myProject).getApplications());
    myApplications.addItemListener(e -> checkOkAction());

    myCommandLabel.setLabelFor(myCommand);
    myCommand.getEditorComponent().addDocumentListener(new DocumentListener() {
      @Override
      public void documentChanged(@NotNull DocumentEvent event) {
        checkOkAction();
      }
    });
    installCommandCompletion();

    myVMOptionsLabel.setLabelFor(myVMOptions);
    installVMOptionsCompletion();

    setTitle(GrailsBundle.message("dialog.title.run.grails.command"));
    init();
    checkOkAction();
  }

  private void createUIComponents() {
    myCommand = new EditorComboBoxWithHistory(myProject, "grails.command.history");
    myVMOptions = new EditorComboBoxWithHistory(myProject, "grails.vmoptions.history");
  }

  @Override
  protected @Nullable JComponent createCenterPanel() {
    return myMainPanel;
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return getSelectedApplicationNullable() == null ? myApplications : myCommand;
  }

  private void checkOkAction() {
    setOKActionEnabled(isOKEnabled());
  }

  private boolean isOKEnabled() {
    if (!StringUtil.isEmptyOrSpaces(getCommandString())) {
      final GrailsApplication selectedApplication = getSelectedApplicationNullable();
      if (selectedApplication != null) {
        if (GrailsCommandExecutor.getGrailsExecutor(selectedApplication) != null) {
          return true;
        }
      }
    }
    return false;
  }

  private @Nullable GrailsApplication getSelectedApplicationNullable() {
    return myApplications.getSelectedApplication();
  }

  public @NotNull GrailsApplication getSelectedApplication() {
    return Objects.requireNonNull(getSelectedApplicationNullable());
  }

  public @NotNull GrailsRunCommandDialog setSelectedApplication(@Nullable GrailsApplication application) {
    myApplications.setSelectedApplication(application);
    return this;
  }

  public @NotNull String getCommandString() {
    return myCommand.getText();
  }

  public @NotNull String getVMOptionsString() {
    return myVMOptions.getText();
  }

  public @NotNull MvcCommand getCommand() {
    return MvcCommand.parse(getCommandString()).setVmOptions(getVMOptionsString());
  }

  @Override
  protected void doOKAction() {
    myCommand.save();
    myVMOptions.save();
    super.doOKAction();
  }

  private void installCommandCompletion() {
    new TextFieldCompletionProvider() {
      @Override
      protected void addCompletionVariants(@NotNull String text, int offset, @NotNull String prefix, @NotNull CompletionResultSet result) {
        result.addAllElements(GrailsCommandCompletionUtil.collectVariants(getSelectedApplicationNullable(), text, offset, prefix));
      }
    }.apply(myCommand.getEditorComponent());
  }

  private void installVMOptionsCompletion() {
    new TextFieldCompletionProvider() {
      @Override
      protected void addCompletionVariants(@NotNull String text, int offset, @NotNull String prefix, @NotNull CompletionResultSet result) {
        if (prefix.startsWith("-D")) {
          result.addAllElements(GrailsCommandCompletionUtil.SYSTEM_PROPERTIES_VARIANTS.getValue());
        }
      }
    }.apply(myVMOptions.getEditorComponent());
  }
}

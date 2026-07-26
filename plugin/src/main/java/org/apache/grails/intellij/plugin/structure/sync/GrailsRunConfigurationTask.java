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

package org.apache.grails.intellij.plugin.structure.sync;

import com.intellij.compiler.options.CompileStepBeforeRun;
import com.intellij.compiler.options.CompileStepBeforeRunNoErrorCheck;
import com.intellij.execution.RunManager;
import com.intellij.execution.RunManagerEx;
import com.intellij.execution.RunnerAndConfigurationSettings;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.runner.GrailsRunConfiguration;
import org.apache.grails.intellij.plugin.runner.GrailsRunConfigurationType;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;

public class GrailsRunConfigurationTask extends GrailsApplicationBackgroundTask {

  public GrailsRunConfigurationTask(Project project) {
    super(project, GrailsBundle.message("progress.title.check.run.configuration"));
  }

  @Override
  protected void run(@NotNull GrailsApplication application, @NotNull ProgressIndicator indicator) {
    if (application.getGrailsVersion().isAtLeast(Version.GRAILS_6_0)) return;

    final GrailsRunConfigurationType configurationType = GrailsRunConfigurationType.getInstance();
    final RunManager runManager = RunManager.getInstance(getProject());
    for (final RunConfiguration runConfiguration : runManager.getConfigurationsList(configurationType)) {
      if (runConfiguration instanceof GrailsRunConfiguration grailsConfiguration) {
        if (grailsConfiguration.getGrailsApplicationNullable() == application) {
          final MvcCommand command = grailsConfiguration.getGrailsCommandNullable();
          if (command != null && "run-app".equals(command.getCommand())) {
            // configuration already exists
            return;
          }
        }
      }
    }
    final ConfigurationFactory factory = configurationType.getConfigurationFactories()[0];
    final RunnerAndConfigurationSettings runSettings = runManager.createConfiguration("Grails: " + application.getName(), factory);
    final GrailsRunConfiguration configuration = (GrailsRunConfiguration)runSettings.getConfiguration();
    configuration.setGrailsApplication(application);
    runManager.addConfiguration(runSettings);
    RunManagerEx.disableTasks(getProject(), configuration, CompileStepBeforeRun.ID, CompileStepBeforeRunNoErrorCheck.ID);
    ApplicationManager.getApplication().invokeLater(() -> runManager.setSelectedConfiguration(runSettings));
  }
}

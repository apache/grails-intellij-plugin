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
package org.apache.grails.intellij.plugin.runner;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationType;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.structure.Grails9Projects;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.util.version.Version;

import javax.swing.Icon;

public final class GrailsRunConfigurationType implements ConfigurationType {
  private final GrailsConfigurationFactory myConfigurationFactory = new GrailsConfigurationFactory(this);

  @Override
  public @NotNull String getDisplayName() {
    return GrailsBundle.message("library.name");
  }

  @Override
  public String getConfigurationTypeDescription() {
    return GrailsBundle.message("library.name");
  }

  @Override
  public Icon getIcon() {
    return GroovyMvcIcons.Grails;
  }

  @Override
  public @NonNls @NotNull String getId() {
    return "GrailsRunConfigurationType";
  }

  @Override
  public ConfigurationFactory[] getConfigurationFactories() {
    return new ConfigurationFactory[]{myConfigurationFactory};
  }

  @Override
  public String getHelpTopic() {
    return "reference.dialogs.rundebug.GrailsRunConfigurationType";
  }

  /**
   * Whether Grails run configurations (run-app and other Grails commands) are offered for the application.
   * Grails 6+ applications run them through the Grails shell (org.grails:grails-shell for Grails 6,
   * org.apache.grails:grails-shell-cli for Apache Grails 7+), so they are runnable whenever the Gradle
   * import resolved that shell.
   */
  /**
   * @return whether a Grails run configuration can run {@code application}: up to Grails 6 always, from Grails 9 on
   * always too, since {@code run-app} is the Gradle task {@code bootRun} there, which needs no Grails command line, and
   * in between when the Grails shell can be started
   */
  public static boolean isRunnable(@NotNull GrailsApplication application) {
    return application.getGrailsVersion().isLessThan(Version.GRAILS_6_0)
           || Grails9Projects.isGrails9OrLater(application)
           || GrailsCommandExecutor.getGrailsExecutor(application) != null;
  }

  public static GrailsRunConfigurationType getInstance() {
    return ConfigurationTypeUtil.findConfigurationType(GrailsRunConfigurationType.class);
  }
}

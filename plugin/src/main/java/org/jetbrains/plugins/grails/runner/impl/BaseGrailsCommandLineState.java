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
package org.jetbrains.plugins.grails.runner.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.configurations.JavaCommandLineState;
import com.intellij.execution.process.KillableColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;

public abstract class BaseGrailsCommandLineState extends JavaCommandLineState {

  private final @NotNull GrailsRunConfiguration myConfiguration;

  protected BaseGrailsCommandLineState(@NotNull ExecutionEnvironment environment,
                                       @NotNull GrailsRunConfiguration configuration) {
    super(environment);
    myConfiguration = configuration;
  }

  public @NotNull GrailsRunConfiguration getConfiguration() {
    return myConfiguration;
  }

  /**
   * @see com.intellij.execution.application.BaseJavaApplicationCommandLineState#startProcess()
   */
  @Override
  protected @NotNull OSProcessHandler startProcess() throws ExecutionException {
    KillableColoredProcessHandler handler = new KillableColoredProcessHandler(createCommandLine());
    ProcessTerminatedListener.attach(handler);
    JavaRunConfigurationExtensionManager.getInstance()
      .attachExtensionsToProcess(getConfiguration(), handler, getRunnerSettings());
    return handler;
  }
}

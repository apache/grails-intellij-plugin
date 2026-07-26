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
package org.apache.grails.intellij.plugin.runner.impl;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.runner.GrailsCommandLineExecutor;
import org.apache.grails.intellij.plugin.runner.GrailsRunConfiguration;
import org.apache.grails.intellij.plugin.runner.util.GrailsExecutionUtils;

public class GrailsRunAppCommandLineState extends GrailsCommandLineState {

  public GrailsRunAppCommandLineState(@NotNull ExecutionEnvironment environment,
                                      @NotNull GrailsRunConfiguration configuration,
                                      @NotNull GrailsCommandLineExecutor executor) throws ExecutionException {
    super(environment, configuration, executor);
  }

  @Override
  public @NotNull ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner)
    throws ExecutionException {
    ExecutionResult result = super.execute(executor, runner);
    ProcessHandler handler = result.getProcessHandler();
    if (handler != null && getConfiguration().isLaunchBrowser()) {
      handler.addProcessListener(
        GrailsExecutionUtils.getBrowserLaunchListener(handler, getConfiguration().getLaunchBrowserUrl()));
    }
    return result;
  }
}

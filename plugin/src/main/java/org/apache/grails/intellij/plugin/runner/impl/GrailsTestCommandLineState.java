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

package org.apache.grails.intellij.plugin.runner.impl;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerConsoleView;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.runner.GrailsCommandLineExecutor;
import org.apache.grails.intellij.plugin.runner.GrailsRerunFailedTestsAction;
import org.apache.grails.intellij.plugin.runner.GrailsRunConfiguration;
import org.apache.grails.intellij.plugin.tests.runner.GrailsUrlProvider;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;

import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;

public class GrailsTestCommandLineState extends GrailsTestAppCommandLineState {

  public static final String FRAMEWORK_NAME = "GrailsTests";
  public static final String GRAILS_LISTENER_NAME = "org.apache.grails.intellij.lib.grails.rt.GrailsIdeaTestListener";

  public GrailsTestCommandLineState(@NotNull ExecutionEnvironment environment,
                                    @NotNull GrailsRunConfiguration configuration,
                                    @NotNull GrailsCommandLineExecutor executor) throws ExecutionException {
    super(environment, configuration, executor);
  }

  @Override
  protected @NotNull JavaParameters doCreateJavaParameters(@NotNull MvcCommand command) throws ExecutionException {
    final JavaParameters parameters = super.doCreateJavaParameters(command);
    getExecutor().addListener(parameters, GRAILS_LISTENER_NAME);
    return parameters;
  }

  @Override
  public @NotNull ExecutionResult execute(@NotNull Executor executor, @NotNull ProgramRunner<?> runner) throws ExecutionException {
    final ProcessHandler processHandler = startProcess();
    final SMTRunnerConsoleProperties properties = new SMTRunnerConsoleProperties(getConfiguration(), FRAMEWORK_NAME, executor) {
      @Override
      public @NotNull SMTestLocator getTestLocator() {
        return GrailsUrlProvider.INSTANCE;
      }
    };
    final SMTRunnerConsoleView consoleView = (SMTRunnerConsoleView)SMTestRunnerConnectionUtil.createAndAttachConsole(
      FRAMEWORK_NAME, processHandler, properties
    );

    // See #IDEA-62538. Grails can ask some question, but it will not be displayed to user because question hasn't  '\n' at end.
    final OutputStream input = processHandler.getProcessInput();
    try (PrintStream ps = new PrintStream(input, false, StandardCharsets.UTF_8)) {
      ps.print("n\nn\nn\nn\nn\nn\nn\nn\nn\nn\n");
      ps.flush();
    }

    final GrailsRerunFailedTestsAction rerunFailedTestsAction = new GrailsRerunFailedTestsAction(consoleView, consoleView.getProperties());
    rerunFailedTestsAction.setModelProvider(consoleView::getResultsViewer);

    DefaultExecutionResult result = new DefaultExecutionResult(consoleView, processHandler, createActions(consoleView, processHandler));
    result.setRestartActions(rerunFailedTestsAction);
    return result;
  }
}

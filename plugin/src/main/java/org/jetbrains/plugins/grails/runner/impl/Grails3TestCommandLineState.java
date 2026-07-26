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
import com.intellij.execution.Executor;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.GeneralToSMTRunnerEventsConvertor;
import com.intellij.execution.testframework.sm.runner.SMTestProxy;
import com.intellij.execution.testframework.sm.runner.ui.SMTRunnerUIActionsHandler;
import com.intellij.execution.testframework.sm.runner.ui.SMTestRunnerResultsForm;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.util.Key;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleConsoleProperties;
import org.jetbrains.plugins.gradle.execution.test.runner.GradleTestsExecutionConsole;
import org.jetbrains.plugins.gradle.execution.test.runner.events.GradleTestsExecutionConsoleOutputProcessor;
import org.jetbrains.plugins.grails.runner.GrailsCommandLineExecutor;
import org.jetbrains.plugins.grails.runner.GrailsRunConfiguration;
import org.jetbrains.plugins.grails.runner.GrailsRunnerSetup;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

public class Grails3TestCommandLineState extends GrailsTestAppCommandLineState {

  private volatile MvcCommand myProxyCommand;

  public Grails3TestCommandLineState(@NotNull ExecutionEnvironment environment,
                                     @NotNull GrailsRunConfiguration configuration,
                                     @NotNull GrailsCommandLineExecutor executor) throws ExecutionException {
    super(environment, configuration, executor);
  }

  @Override
  public @NotNull MvcCommand getCommand() {
    MvcCommand cached = myProxyCommand;
    if (cached != null) return cached;
    synchronized (this) {
      if (myProxyCommand == null) {
        myProxyCommand = createProxyCommand();
      }
      return myProxyCommand;
    }
  }

  /**
   * Wraps the real command in Grails' {@code intellij-command-proxy}, which runs it through Gradle
   * with our init script attached so test events come back in a form the Gradle test console reads.
   */
  private @NotNull MvcCommand createProxyCommand() {
    MvcCommand originalCommand = super.getCommand();
    MvcCommand command = new MvcCommand("intellij-command-proxy");
    command.getArgs().add(originalCommand.getCommand());
    command.getArgs().addAll(originalCommand.getArgs());
    command.setEnv(originalCommand.getEnv());
    command.setVmOptions(originalCommand.getVmOptions());
    command.setEnvVariables(originalCommand.getEnvVariables());
    command.setPassParentEnvs(originalCommand.isPassParentEnvs());
    command.getEnvVariables().put(GrailsRunnerSetup.PATH_TO_GRADLE_INIT_SCRIPT_KEY,
                                 GrailsRunnerSetup.getPathToGradleInitScript());
    return command;
  }

  @Override
  protected @NotNull ConsoleView createConsole(@NotNull Executor executor) {
    String splitterPropertyName = SMTestRunnerConnectionUtil.getSplitterPropertyName(GrailsRunnerSetup.TEST_FRAMEWORK_NAME);
    GradleConsoleProperties consoleProperties =
      new GradleConsoleProperties(getConfiguration(), GrailsRunnerSetup.TEST_FRAMEWORK_NAME, executor);
    GradleTestsExecutionConsole console = new GradleTestsExecutionConsole(consoleProperties, splitterPropertyName);
    console.setHelpId("reference.runToolWindow.testResultsTab");
    console.initUI();
    console.addAttachToProcessListener(handler -> {
      SMTestRunnerResultsForm resultsViewer = console.getResultsViewer();
      resultsViewer.addEventsListener(new SMTRunnerUIActionsHandler(consoleProperties));

      SMTestProxy.SMRootTestProxy rootNode = resultsViewer.getTestsRootNode();
      rootNode.setHandler(handler);

      GeneralToSMTRunnerEventsConvertor eventsProcessor =
        new GeneralToSMTRunnerEventsConvertor(consoleProperties.getProject(), rootNode, GrailsRunnerSetup.TEST_FRAMEWORK_NAME);
      eventsProcessor.addEventsListener(resultsViewer);
      eventsProcessor.onStartTesting();

      handler.addProcessListener(new ProcessAdapter() {
        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
          eventsProcessor.onFinishTesting();
        }

        @Override
        @SuppressWarnings("rawtypes") // ProcessListener declares a raw Key
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key outputType) {
          GradleTestsExecutionConsoleOutputProcessor.onOutput(console, event.getText(), outputType);
        }
      });
    });
    return console;
  }
}

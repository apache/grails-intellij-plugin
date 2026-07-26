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

package org.jetbrains.plugins.grails.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.actions.JavaRerunFailedTestsAction;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.testframework.AbstractTestProxy;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.openapi.ui.ComponentContainer;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GrailsRerunFailedTestsAction extends JavaRerunFailedTestsAction {
  public GrailsRerunFailedTestsAction(@NotNull ComponentContainer componentContainer, @NotNull TestConsoleProperties consoleProperties) {
    super(componentContainer, consoleProperties);
  }

  @Override
  protected MyRunProfile getRunProfile(@NotNull ExecutionEnvironment environment) {
    final GrailsRunConfiguration configuration = ((GrailsRunConfiguration)myConsoleProperties.getConfiguration());
    return new MyRunProfile(configuration) {
      @Override
      public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment env) throws ExecutionException {
        GrailsRunConfiguration clone = (GrailsRunConfiguration)configuration.clone();

        String s = clone.getProgramParameters();
        if (s == null) s = "";
        s = s.replaceAll("((^| )(\\p{javaJavaIdentifierStart}\\p{javaJavaIdentifierPart}*\\.)*[*\\p{javaJavaIdentifierStart}][*\\p{javaJavaIdentifierPart}]*)+( |$)", " ");

        List<AbstractTestProxy> failedTests = getFailedTests(configuration.getProject());

        String cmdLine = null;

        if (failedTests.size() == 1) {
          AbstractTestProxy testProxy = failedTests.get(0);
          AbstractTestProxy parent = testProxy.getParent();
          if (parent != null) {
            cmdLine = s + ' ' + parent.getName() + '.' + testProxy.getName();
          }
        }

        if (cmdLine == null) {
          Set<String> failedTestsNames = new HashSet<>();

          for (AbstractTestProxy failedTest : failedTests) {
            AbstractTestProxy parent = failedTest.getParent();
            if (parent != null) {
              failedTestsNames.add(parent.getName());
            }
          }

          cmdLine = s + ' ' + StringUtil.join(failedTestsNames, " ");
        }

        clone.setProgramParameters(cmdLine);
        return clone.getState(executor, environment);
      }
    };
  }

}

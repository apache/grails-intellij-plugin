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

package org.apache.grails.intellij.plugin.runner;

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaCommandLine;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.application.ReadAction;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.config.GrailsStructure;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.impl.Grails2Application;

public final class GrailsAppEngineDebuggerRunner extends GenericDebuggerRunner {

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    if (!(profile instanceof GrailsRunConfiguration runConfiguration)) return false;
    if (!executorId.equals(DefaultDebugExecutor.EXECUTOR_ID)) return false;
    GrailsApplication application = runConfiguration.getGrailsApplicationNullable();
    if (!(application instanceof Grails2Application)) return false;
    return ReadAction.compute(() -> {
      GrailsStructure structure = GrailsStructure.getInstance(((Grails2Application)application).getModule());
      return structure != null && structure.isPluginInstalled("app-engine");
    });
  }

  @Override
  public @NotNull String getRunnerId() {
    return getClass().getSimpleName();
  }

  @Override
  protected RunContentDescriptor createContentDescriptor(@NotNull RunProfileState state, @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    ((JavaCommandLine)state).getJavaParameters().getVMParametersList().add("-Dappengine.debug=true");
    return attachVirtualMachine(state, environment, new RemoteConnection(true, "127.0.0.1", "9999", false), true);
  }
}

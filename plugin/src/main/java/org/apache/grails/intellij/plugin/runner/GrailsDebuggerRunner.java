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

import com.intellij.debugger.impl.GenericDebuggerRunner;
import com.intellij.debugger.impl.RemoteConnectionBuilder;
import com.intellij.debugger.settings.DebuggerSettings;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RemoteConnection;
import com.intellij.execution.configurations.RunProfile;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.ui.RunContentDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.PathsList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.runner.impl.GrailsCommandLineState;
import org.apache.grails.intellij.plugin.runner.impl.GrailsRunAppCommandLineState;
import org.apache.grails.intellij.plugin.runner.impl.GrailsTestAppCommandLineState;
import org.apache.grails.intellij.plugin.runner.impl.GrailsTestCommandLineState;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.impl.Grails2Application;
import org.apache.grails.intellij.plugin.util.version.Version;

import java.util.List;

public class GrailsDebuggerRunner extends GenericDebuggerRunner {

  private static final String DEBUG_KEY_V2_SINGLE = "--debug";
  private static final String DEBUG_KEY_V2_FORKED = "--debug-fork";
  private static final String DEBUG_KEY_V3 = "--debug-jvm";
  private static final long POLL_TIMEOUT = 5 * 60 * 1000L; // 5 minutes

  @Override
  public @NotNull String getRunnerId() {
    return getClass().getName();
  }

  @Override
  public boolean canRun(@NotNull String executorId, @NotNull RunProfile profile) {
    if (!DefaultDebugExecutor.EXECUTOR_ID.equals(executorId) || !(profile instanceof GrailsRunConfiguration configuration)) {
      return false;
    }
    GrailsApplication application = configuration.getGrailsApplicationNullable();
    return application instanceof Grails3Application || application instanceof Grails2Application;
  }

  @Override
  protected @Nullable RunContentDescriptor createContentDescriptor(@NotNull RunProfileState state,
                                                                   @NotNull ExecutionEnvironment environment)
    throws ExecutionException {
    if (!(state instanceof GrailsCommandLineState commandLineState)
        || !(state instanceof GrailsRunAppCommandLineState || state instanceof GrailsTestAppCommandLineState)) {
      return super.createContentDescriptor(state, environment);
    }

    RemoteConnection connection;
    GrailsApplication application = commandLineState.getApplication();
    if (application instanceof Grails2Application) {
      if (GrailsRunnerSetup.isForkedDebug(commandLineState)) {
        // Grails forks the JVM itself, so the debug agent options have to be handed to it as a
        // build property rather than passed on our own command line.
        commandLineState.getCommand().getArgs().removeIf(DEBUG_KEY_V2_FORKED::equals);
        JavaParameters javaParams = commandLineState.getJavaParameters();
        DebugOptions options = createOptionsAndConnection(javaParams, false, environment.getProject());
        String propertyName = commandLineState instanceof GrailsTestCommandLineState
                              ? "grails.project.fork.test.debugArgs"
                              : "grails.project.fork.run.debugArgs";
        javaParams.getVMParametersList().addProperty(propertyName, options.vmOptions());
        connection = options.connection();
      }
      else {
        commandLineState.getCommand().getArgs().removeIf(DEBUG_KEY_V2_SINGLE::equals);
        JavaParameters javaParams = commandLineState.getJavaParameters();
        DebugOptions options = createOptionsAndConnection(javaParams, true, environment.getProject());
        javaParams.getVMParametersList().addParametersString(options.vmOptions());
        connection = options.connection();
      }
    }
    else if (application instanceof Grails3Application grails3) {
      if (grails3.getGrailsVersion().isAtLeast(Version.GRAILS_3_1_5) && !Registry.is("grails.simple.debug")) {
        commandLineState.getCommand().getArgs().removeIf(DEBUG_KEY_V3::equals);
        JavaParameters javaParams = commandLineState.getJavaParameters();
        DebugOptions options = createOptionsAndConnection(javaParams, true, environment.getProject());
        String existing = javaParams.getEnv().get(GrailsRunnerSetup.OPTS_KEY);
        javaParams.getEnv().put(GrailsRunnerSetup.OPTS_KEY,
                                StringUtil.isEmptyOrSpaces(existing)
                                ? options.vmOptions()
                                : existing + " " + options.vmOptions());
        connection = options.connection();
      }
      else {
        // Let Grails open the debug port itself and just attach to its fixed default.
        List<String> args = commandLineState.getCommand().getArgs();
        if (!args.contains(DEBUG_KEY_V3)) args.add(DEBUG_KEY_V3);
        connection = new RemoteConnection(true, "localhost", "5005", false);
      }
    }
    else {
      return null;
    }

    return attachVirtualMachine(commandLineState, environment, connection, POLL_TIMEOUT);
  }

  private static @NotNull DebugOptions createOptionsAndConnection(@NotNull JavaParameters javaParams,
                                                                  boolean asyncDebugger,
                                                                  @NotNull Project project) throws ExecutionException {
    // RemoteConnectionBuilder writes the agent options into the parameters it is given; hand it a
    // delegate so only the resulting option string is taken, not the mutations.
    JavaParameters javaParamsDelegate = new JavaParameters() {
      @Override
      public PathsList getClassPath() {
        return javaParams.getClassPath();
      }
    };
    javaParamsDelegate.setJdk(javaParams.getJdk());
    RemoteConnection connection = new RemoteConnectionBuilder(false, DebuggerSettings.SOCKET_TRANSPORT, null)
      .suspend(false)
      .asyncAgent(asyncDebugger)
      .project(project)
      .create(javaParamsDelegate);
    return new DebugOptions(connection, javaParamsDelegate.getVMParametersList().getParametersString());
  }

  private record DebugOptions(@NotNull RemoteConnection connection, @NotNull String vmOptions) {
  }
}

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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.projectRoots.JavaSdkVersion;
import com.intellij.openapi.projectRoots.JavaSdkVersionUtil;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.runner.impl.Grails3TestCommandLineState;
import org.apache.grails.intellij.plugin.runner.impl.GrailsCommandLineState;
import org.apache.grails.intellij.plugin.runner.impl.GrailsRunAppCommandLineState;
import org.apache.grails.intellij.plugin.runner.impl.GrailsTestCommandLineState;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.apache.grails.intellij.plugin.mvc.ConsoleProcessDescriptor;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;

public abstract class GrailsCommandLineExecutor extends GrailsCommandExecutor {

  private static final Logger LOG = Logger.getInstance(GrailsCommandLineExecutor.class);

  private static final String RUN_APP_PATTERN = "\\s*(-D[^\\s]+\\s+)*run-app(\\s.*)?";
  private static final String TEST_APP_PATTERN = "\\s*(-D[^\\s]+\\s+)*test-app(\\s.*)?";

  private @Nullable RunProfileState getSpecialState(@NotNull GrailsRunConfiguration configuration,
                                                    @NotNull ExecutionEnvironment environment) throws ExecutionException {
    final String programParameters = configuration.getProgramParameters();
    if (programParameters == null) return null;

    final Version version = configuration.getGrailsApplication().getGrailsVersion();
    if (programParameters.matches(TEST_APP_PATTERN)) {
      if (version.compareTo(Version.GRAILS_3_0) >= 0) {
        return new Grails3TestCommandLineState(environment, configuration, this);
      }
      else if (version.compareTo(Version.GRAILS_1_2) >= 0) {
        return new GrailsTestCommandLineState(environment, configuration, this);
      }
    }
    else if (programParameters.matches(RUN_APP_PATTERN)) {
      return new GrailsRunAppCommandLineState(environment, configuration, this);
    }
    return null;
  }

  @Override
  public @Nullable RunProfileState getState(@NotNull GrailsRunConfiguration configuration,
                                            @NotNull Executor executor,
                                            @NotNull ExecutionEnvironment environment) throws ExecutionException {
    RunProfileState state = getSpecialState(configuration, environment);
    return state == null ? new GrailsCommandLineState(environment, configuration, this) : state;
  }

  @Override
  public @Nullable ConsoleProcessDescriptor execute(@NotNull GrailsApplication application,
                                                    @NotNull MvcCommand command,
                                                    @Nullable Runnable onDone,
                                                    boolean close,
                                                    String... input) throws ExecutionException {
    final Project project = application.getProject();
    return GrailsConsole.getInstance(project).executeProcess(createCommandLine(application, command), onDone, true, close, input);
  }

  public GeneralCommandLine createCommandLine(@NotNull GrailsApplication application,
                                              @NotNull MvcCommand command) throws ExecutionException {
    JavaParameters params = createJavaParameters(application, command);
    params.setUseDynamicClasspath(application.getProject());
    allowNativeAccess(params);
    return params.toCommandLine();
  }

  public abstract @NotNull JavaParameters createJavaParameters(@NotNull GrailsApplication grailsApplication,
                                                               @NotNull MvcCommand command) throws ExecutionException;

  /**
   * Allows the Grails CLI's terminal library (JLine) to use restricted foreign-memory methods
   * without printing {@code WARNING: ... MemorySegment ...} noise on recent JDKs. The flag is
   * only added from JDK 22 on: that is where the FFM API is final and JLine's foreign-memory
   * terminal provider (the source of the warnings) kicks in, and it keeps the launch unchanged for
   * the JDK 8 and 11 era this plugin still supports, where the option does not exist (it was
   * introduced in JDK 17) and would prevent the JVM from starting. When the JDK cannot be
   * determined, or the user already set the flag, the parameters are left untouched. Besides
   * silencing the warnings, this keeps them out of stderr, so the console's error detection (and
   * auto-hide on success) is not tripped by CLI noise.
   */
  public static void allowNativeAccess(@NotNull JavaParameters params) {
    for (String parameter : params.getVMParametersList().getList()) {
      if (parameter.startsWith("--enable-native-access")) {
        return;
      }
    }
    Sdk jdk = params.getJdk();
    if (jdk == null) return;
    JavaSdkVersion version = JavaSdkVersionUtil.getJavaSdkVersion(jdk);
    if (version != null && version.isAtLeast(JavaSdkVersion.JDK_22)) {
      LOG.debug("Allowing native access for Grails CLI on " + jdk.getName());
      params.getVMParametersList().add("--enable-native-access=ALL-UNNAMED");
    }
  }

  public void addListener(@NotNull JavaParameters params, @NotNull String listener) {
    String listeners = params.getVMParametersList().getPropertyValue("grails.build.listeners");

    if (listeners != null) {
      if (listeners.startsWith("\"") && listeners.endsWith("\"")) {
        listeners = "\"" + listeners.substring(1, listeners.length() - 2) + "," + listener + "\"";
      }
      else {
        listeners = "\"" + listeners + "," + listener + "\"";
      }
    }
    else {
      listeners = listener;
    }

    params.getVMParametersList().replaceOrAppend("grails.build.listeners", "-Dgrails.build.listeners=" + listeners);
  }
}

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
import com.intellij.execution.JavaRunConfigurationExtensionManager;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.ParametersList;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.execution.GradleInitScriptUtil;
import org.apache.grails.intellij.plugin.runner.impl.GrailsCommandLineState;
import org.apache.grails.intellij.plugin.runner.impl.GrailsTestCommandLineState;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.impl.Grails2Application;
import org.apache.grails.intellij.plugin.util.version.Version;

/**
 * Wiring between a {@link GrailsRunConfiguration} and the {@link JavaParameters} the platform
 * actually launches — chiefly how VM options reach a forked Grails process, which differs per
 * Grails generation.
 */
public final class GrailsRunnerSetup {

  private GrailsRunnerSetup() {
  }

  @ApiStatus.Internal
  public static final String OPTS_KEY = "GRAILS_FORK_OPTS";

  private static final String PLAIN_OUTPUT_KEY_V3 = "--plain-output";

  @ApiStatus.Internal
  public static final String TEST_FRAMEWORK_NAME = "GrailsTests";

  @ApiStatus.Internal
  public static final String PATH_TO_GRADLE_INIT_SCRIPT_KEY = "INTELLIJ_GRADLE_INIT_SCRIPT";

  /** Written on first use only: creating the init script touches the filesystem. */
  private static final class InitScriptHolder {
    static final String PATH = GradleInitScriptUtil.createTestInitScript().toString();
  }

  @ApiStatus.Internal
  public static @NotNull String getPathToGradleInitScript() {
    return InitScriptHolder.PATH;
  }

  public static boolean canPassVmArgs(@Nullable GrailsApplication application) {
    return application instanceof Grails2Application
           || application instanceof Grails3Application grails3
              && grails3.getGrailsVersion().isAtLeast(Version.GRAILS_3_1_5);
  }

  public static void setupJavaParameters(@NotNull GrailsRunConfiguration configuration,
                                        @NotNull GrailsCommandLineState state,
                                        @NotNull JavaParameters params) throws ExecutionException {
    // Run the extensions against a throwaway JavaParameters so we can collect the VM options they
    // contribute and forward them the way this Grails version expects, rather than letting them
    // land on the launcher process.
    JavaParameters delegate = new JavaParameters();
    delegate.setJdk(params.getJdk());
    delegate.getClassPath().addAll(params.getClassPath().getPathList());
    JavaRunConfigurationExtensionManager.getInstance()
      .updateJavaParameters(configuration, delegate, state.getRunnerSettings(), state.getEnvironment().getExecutor());
    String vmOptions = delegate.getVMParametersList().getParametersString();

    GrailsApplication application = configuration.getGrailsApplication();
    if (application instanceof Grails2Application) {
      if (isForkedDebug(state)) {
        String propertyName = state instanceof GrailsTestCommandLineState
                              ? "grails.project.fork.test.debugArgs"
                              : "grails.project.fork.run.debugArgs";
        params.getVMParametersList().addProperty(propertyName, vmOptions);
      }
      else {
        params.getVMParametersList().addParametersString(vmOptions);
      }
    }
    else if (application instanceof Grails3Application grails3
             && grails3.getGrailsVersion().isAtLeast(Version.GRAILS_3_1_5)) {
      String existing = params.getEnv().get(OPTS_KEY);
      params.getEnv().put(OPTS_KEY, StringUtil.isEmptyOrSpaces(existing) ? vmOptions : existing + " " + vmOptions);
    }
  }

  @ApiStatus.Internal
  public static boolean isForkedDebug(@NotNull GrailsCommandLineState state) {
    if (!(state.getApplication() instanceof Grails2Application application)) return false;
    Version version = application.getGrailsVersion();
    boolean forTests = state instanceof GrailsTestCommandLineState;
    // There is a bug (https://github.com/grails/grails-core/issues/5641) in grails < 2.3.10
    // that doesn't allow us to debug "test-app" in forked mode.
    // We should have ability to debug "run-*" commands in forked mode starting from 2.3.0.
    //
    // But there is another bug (https://github.com/grails/grails-core/issues/3294)
    // that makes no sense to disable forked mode only for tests,
    // i.e. forked mode must be disabled for "run-*" too.
    //
    // Finally we will allow debug tests for >=2.3.10 and debug "run-*" for >=2.3.5,
    // and force users to disable forked mode in older versions.
    if (forTests) {
      return version.isAtLeast(Version.GRAILS_2_3_10) && application.isTestForked();
    }
    return version.isAtLeast(Version.GRAILS_2_3_5) && application.isRunForked();
  }

  @ApiStatus.Internal
  public static void addPlainOutput(@NotNull ParametersList parameters) {
    if (Registry.is("grails.add.plain.output") && !parameters.hasParameter(PLAIN_OUTPUT_KEY_V3)) {
      parameters.add(PLAIN_OUTPUT_KEY_V3);
    }
  }
}

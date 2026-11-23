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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.externalSystem.util.task.TaskExecutionSpec;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.execution.ParametersListUtil;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;
import org.apache.grails.intellij.plugin.runner.GrailsRunConfiguration;
import org.apache.grails.intellij.plugin.structure.Grails9Projects;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@code run-app} of a Grails run configuration for a project of Grails 9 and later: the Gradle task
 * {@code bootRun}, run by the Gradle support of the IDE.
 * <p>
 * The command line of Grails 9 runs {@code run-app} as {@code gradlew bootRun} too, and its build commands are
 * Gradle tasks that an IDE runs itself (upgrade note 51 of the Grails guide). Run that way, the application is
 * debugged as the IDE debugs every task that starts a JVM, on a port of its choosing, and Stop cancels the build,
 * which ends the application. Started through the command line instead, debugging would need the fixed port of
 * {@code run-app --debug-jvm}, and the application would be one more process away. {@code .grails/run/app.json},
 * the record of {@code run-app}, is not written: the IDE stops what it starts, and {@code grails stop-app} does not
 * stop an application that {@code bootRun} started, as it does not for {@code ./gradlew bootRun}.
 * <p>
 * The options of {@code run-app} that {@code bootRun} has an equivalent for are given to it as the command line
 * gives them; the others are refused before anything runs.
 */
public final class Grails9RunApp {

  public static final String COMMAND = "run-app";

  public static final String TASK = "bootRun";

  /**
   * On the Gradle run configuration of a {@code run-app} that opens the browser: the address to open, or an empty
   * string for the one the application prints when it is up.
   */
  public static final Key<String> LAUNCH_BROWSER_URL = Key.create("grails.run.app.launch.browser.url");

  private static final Map<String, String> ENVIRONMENTS = Map.of("dev", "development", "prod", "production", "test", "test");

  private Grails9RunApp() {
  }

  public static boolean isRunApp(@NotNull MvcCommand command) {
    return COMMAND.equals(command.getCommand());
  }

  /**
   * @return whether a Grails run configuration of {@code application} runs {@code command} as the Gradle task
   * {@code bootRun}: {@code run-app} of every project of Grails 9 and later, whatever runs its other commands, since
   * {@code bootRun} needs neither the command line of Grails 9 nor the Grails shell
   */
  public static boolean runsAsBootRun(@NotNull GrailsApplication application, @NotNull MvcCommand command) {
    return isRunApp(command) && Grails9Projects.isGrails9OrLater(application);
  }

  /**
   * Runs the {@code run-app} of {@code configuration} as the Gradle task {@code bootRun} with {@code executor} (Run or
   * Debug), in a run of the Gradle support of its own, named after the configuration.
   */
  public static void run(@NotNull GrailsRunConfiguration configuration, @NotNull Executor executor) throws ExecutionException {
    run(configuration.getGrailsApplication(), configuration.getGrailsCommand(), configuration.getName(),
        configuration.isLaunchBrowser() ? StringUtil.notNullize(configuration.getLaunchBrowserUrl()) : null,
        executor);
  }

  /**
   * The settings of the Gradle run of {@code command}, a {@code run-app}, in the project of {@code application}.
   *
   * @throws ExecutionException for an option of {@code run-app} that {@code bootRun} has no equivalent for, and
   *                            for VM options, which are not the application's when Gradle starts it
   */
  public static @NotNull ExternalSystemTaskExecutionSettings createSettings(@NotNull GrailsApplication application,
                                                                            @NotNull MvcCommand command,
                                                                            @NotNull String name) throws ExecutionException {
    if (!StringUtil.isEmptyOrSpaces(command.getVmOptions())) {
      throw new ExecutionException(GrailsBundle.message("dialog.message.grails9.run.app.vm.options", command.getVmOptions()));
    }
    ExternalSystemTaskExecutionSettings settings = new ExternalSystemTaskExecutionSettings();
    settings.setExternalSystemIdString(GradleConstants.SYSTEM_ID.getId());
    settings.setExternalProjectPath(application.getRoot().getPath());
    settings.setExecutionName(name);
    settings.setTaskNames(List.of(TASK));
    settings.setScriptParameters(ParametersListUtil.join(gradleArguments(command)));
    settings.setEnv(new HashMap<>(command.getEnvVariables()));
    settings.setPassParentEnvs(command.isPassParentEnvs());
    return settings;
  }

  /**
   * The arguments of Gradle for {@code command}, a {@code run-app}: the environment, {@code development} unless
   * one is named, as {@code -Dgrails.env}; {@code -D} and {@code -P} options as they are; {@code --port} and
   * {@code --host}, also with one dash or with {@code =}, as {@code -Dgrails.server.port} and
   * {@code -Dgrails.server.address}, which the Grails Gradle plugin gives the application; and everything after
   * {@code --} as it is. {@code --debug-jvm} is dropped: the Debug action of the IDE debugs the application.
   *
   * @throws ExecutionException for any other option, such as {@code --https}, {@code --detach} or
   *                            {@code --query-tail}, which only the command line has
   */
  public static @NotNull List<String> gradleArguments(@NotNull MvcCommand command) throws ExecutionException {
    List<String> words = new ArrayList<>(command.getProperties());
    words.addAll(command.getArgs());

    List<String> result = new ArrayList<>();
    List<String> passedOn = new ArrayList<>();
    boolean environmentNamed = false;
    for (int i = 0; i < words.size(); i++) {
      String word = words.get(i);
      if (word.equals("--")) {
        passedOn.addAll(words.subList(i + 1, words.size()));
        break;
      }
      if (word.startsWith("-D") || word.startsWith("-P")) {
        String property = word.length() == 2 && i + 1 < words.size() ? words.get(i + 1) : word.substring(2);
        if (word.startsWith("-D") && (property.equals("grails.env") || property.startsWith("grails.env="))) {
          environmentNamed = true;
        }
        result.add(word);
        if (word.length() == 2 && i + 1 < words.size()) {
          result.add(words.get(++i));
        }
        continue;
      }
      if (!word.startsWith("-")) {
        throw new ExecutionException(GrailsBundle.message("dialog.message.grails9.run.app.argument", word));
      }
      String option = StringUtil.trimStart(StringUtil.trimStart(word, "-"), "-");
      String value = null;
      int equals = option.indexOf('=');
      if (equals >= 0) {
        value = option.substring(equals + 1);
        option = option.substring(0, equals);
      }
      switch (option) {
        case "port", "host" -> {
          if (value == null) {
            if (i + 1 >= words.size()) {
              throw new ExecutionException(GrailsBundle.message("dialog.message.grails9.run.app.option.value", word));
            }
            value = words.get(++i);
          }
          result.add((option.equals("port") ? "-Dgrails.server.port=" : "-Dgrails.server.address=") + value);
        }
        case "debug-jvm" -> {
          // the Debug action of the IDE attaches to the application on a port of its own
        }
        default -> throw new ExecutionException(GrailsBundle.message("dialog.message.grails9.run.app.option", word));
      }
    }
    if (!environmentNamed) {
      String environment = command.getEnv();
      result.add(0, "-Dgrails.env=" + (environment == null ? "development" : ENVIRONMENTS.getOrDefault(environment, environment)));
    }
    result.addAll(passedOn);
    return result;
  }

  /**
   * Runs {@code command}, a {@code run-app}, as the Gradle task {@code bootRun} with {@code executor} (Run or Debug).
   * The run is the Gradle support's own, in a tab of its own.
   *
   * @param launchBrowserUrl the address to open once the application is up, an empty string for the one it prints,
   *                         or {@code null} to open none
   */
  static void run(@NotNull GrailsApplication application,
                  @NotNull MvcCommand command,
                  @NotNull String name,
                  @Nullable String launchBrowserUrl,
                  @NotNull Executor executor) throws ExecutionException {
    ExternalSystemTaskExecutionSettings settings = createSettings(application, command, name);
    UserDataHolderBase userData = new UserDataHolderBase();
    if (launchBrowserUrl != null) {
      userData.putUserData(LAUNCH_BROWSER_URL, launchBrowserUrl);
    }
    Project project = application.getProject();
    TaskExecutionSpec spec = TaskExecutionSpec.create(project, GradleConstants.SYSTEM_ID, executor.getId(), settings)
      .withActivateToolWindowBeforeRun(true)
      .withUserData(userData)
      .build();
    ApplicationManager.getApplication().invokeLater(() -> ExternalSystemUtil.runTask(spec), project.getDisposed());
  }
}

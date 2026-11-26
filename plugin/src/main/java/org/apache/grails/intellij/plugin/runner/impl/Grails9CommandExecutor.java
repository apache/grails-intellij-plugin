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
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.util.containers.ContainerUtil;
import org.apache.grails.intellij.lib.gradle.tooling.builder.GrailsCommandLine;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.commands.Grails9CommandCatalog;
import org.apache.grails.intellij.plugin.commands.Grails9CommandLines;
import org.apache.grails.intellij.plugin.commands.Grails9HelpDocument;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;
import org.apache.grails.intellij.plugin.runner.GrailsCommandLineExecutor;
import org.apache.grails.intellij.plugin.runner.GrailsRunConfiguration;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.Grails9Projects;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import static org.apache.grails.intellij.plugin.runner.util.GrailsExecutionUtils.addCommonJvmOptions;

/**
 * Runs the commands of a project of Grails 9 and later in the command line of Grails 9.
 * <p>
 * Grails 9 has no Grails shell. A command is run by the one command line of the project's version,
 * {@code org.apache.grails:grails-cli}, which the Gradle import resolves, with
 * {@code --plain-output --non-interactive}, of those two the ones the command line lists as its options: the console
 * of the IDE is no terminal, and nothing there answers a question. A build of Grails 9 from before that command line
 * has the shell, and its commands run there ({@link Grails9CommandLines}). The class started is the one the
 * {@code Main-Class} attribute of the jar's manifest names, as the Grails wrapper starts it ({@link #mainClass(List)}).
 * The commands that build the project start its Gradle wrapper from there, as they do on a command line (upgrade notes
 * 37, 43 and 51 of the Grails guide).
 * An environment or system properties typed in front of a command, as the Grails shell took them, are given to the
 * command line where the command takes them ({@link #programArguments}).
 * <p>
 * {@code run-app} of a Grails run configuration is the exception, for every project of Grails 9 and later whatever
 * runs its commands: it runs the Gradle task {@code bootRun} through the Gradle support of the IDE
 * ({@link Grails9RunApp}), which debugs and stops the application the way it does every Gradle task.
 */
public final class Grails9CommandExecutor extends GrailsCommandLineExecutor {

  private static final Logger LOG = Logger.getInstance(Grails9CommandExecutor.class);

  /** The option that has the command line write plain text for a program to read. */
  public static final String PLAIN_OUTPUT = "--plain-output";

  /** The option that has the command line never wait for what a user types. */
  public static final String NON_INTERACTIVE = "--non-interactive";

  /** The command that runs a tool of the application or of a plugin, and takes its environment in front of its name. */
  public static final String RUN_TOOL = "run-tool";

  /** The deprecated name of {@link #RUN_TOOL}, which still works and warns; it takes the environment in the same place. */
  public static final String RUN_COMMAND = "run-command";

  private static final String ENVIRONMENT_OPTION = "-Dgrails.env=";

  private static final Map<String, String> ENVIRONMENTS = Map.of("dev", "development", "prod", "production", "test", "test");

  /**
   * For a project of Grails 9 and later whose commands run in the command line of Grails 9 ({@link Grails9CommandLines}):
   * one whose jar answers as that command line does, or for which it is chosen, or which has no Grails shell to fall back to.
   */
  @Override
  public boolean isApplicable(@NotNull GrailsApplication grailsApplication) {
    return Grails9Projects.isGrails9OrLater(grailsApplication) && Grails9CommandLines.runsCommandLine(grailsApplication);
  }

  /**
   * Neither the test proxy nor the debug agent of the Grails shell applies: the command runs as it is typed. {@code run-app}
   * does not come here: the Grails run configuration runs it as the Gradle task {@code bootRun} ({@link Grails9RunApp}).
   */
  @Override
  public @Nullable RunProfileState getState(@NotNull GrailsRunConfiguration configuration,
                                            @NotNull Executor executor,
                                            @NotNull ExecutionEnvironment environment) throws ExecutionException {
    return new GrailsCommandLineState(environment, configuration, this);
  }

  @Override
  public @NotNull JavaParameters createJavaParameters(@NotNull GrailsApplication grailsApplication,
                                                      @NotNull MvcCommand command) throws ExecutionException {
    if (!(grailsApplication instanceof Grails3Application grails9)) {
      throw new ExecutionException(GrailsBundle.message("dialog.message.cannot.execute.grails.command", grailsApplication.getRoot().getPath()));
    }
    List<String> classPath = grails9.getGradleData().getCommandLineUrls();
    if (classPath.isEmpty()) {
      throw new ExecutionException(GrailsBundle.message("dialog.message.grails.command.line.not.resolved",
                                                        GrailsCommandLine.coordinates(grails9.getGradleData().getGrailsVersion())));
    }

    JavaParameters params = new JavaParameters();
    params.setJdk(ProjectRootManager.getInstance(grailsApplication.getProject()).getProjectSdk());
    // what the start scripts of the command line give a command: it is a short-lived process
    params.getVMParametersList().add("-XX:TieredStopAtLevel=1");
    params.getVMParametersList().addParametersString(command.getVmOptions());
    addCommonJvmOptions(params);
    params.getClassPath().addAll(classPath);
    params.setMainClass(mainClass(classPath));
    params.getProgramParametersList().addAll(
      programArguments(command, Grails9CommandLines.getInstance(grailsApplication.getProject()).globalOptions(grailsApplication),
                       () -> Grails9CommandCatalog.getInstance(grailsApplication.getProject()).getDocument(grailsApplication)));
    params.setWorkingDirectory(VfsUtilCore.virtualToIoFile(grailsApplication.getRoot()));
    params.setDefaultCharset(grailsApplication.getProject());
    params.setUseClasspathJar(true);
    return params;
  }

  /**
   * The class that starts the command line on {@code classPath}: the {@code Main-Class} of the manifest of the first
   * jar that names one, which is where Grails names its start class for a tool to read (upgrade note 37 of the Grails
   * guide), or {@link GrailsCommandLine#FALLBACK_MAIN_CLASS} when no jar names one or none can be read.
   */
  public static @NotNull String mainClass(@NotNull List<String> classPath) {
    for (String entry : classPath) {
      File file = new File(entry);
      if (!file.isFile()) continue;
      try (JarFile jar = new JarFile(file, false)) {
        Manifest manifest = jar.getManifest();
        String mainClass = manifest == null ? null : manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
        if (mainClass != null && !mainClass.isBlank()) return mainClass.strip();
      }
      catch (IOException e) {
        LOG.warn("The manifest of " + file + " could not be read", e);
      }
    }
    return GrailsCommandLine.FALLBACK_MAIN_CLASS;
  }

  /**
   * The arguments of the command line for {@code command}.
   * <p>
   * The options of the command line itself come first: {@code globalOptions}, those of {@code --plain-output} and
   * {@code --non-interactive} the command line lists as its own ({@link Grails9CommandLines#globalOptions}). A command
   * line of the Grails shell named the environment ({@code prod war}) and system properties ({@code -Dname=value
   * run-app}) in front of the command. The command line of Grails 9 refuses both there (upgrade note 43), and where it
   * takes them depends on the command ("The Environment and System Properties" in the chapter "The Command Line" of
   * the Grails guide), so they are moved there:
   * <ul>
   *   <li>a command of the command line itself takes them after its name: {@code war -Dgrails.env=production};</li>
   *   <li>a tool of the application or of a plugin takes everything after its name as its own arguments, and would
   *       run in {@code development} with {@code -Dgrails.env=production} as one of them. It is run with
   *       {@code run-tool}, which takes them in front of the name of the tool it runs:
   *       {@code run-tool -Dgrails.env=production dbm-update};</li>
   *   <li>{@code run-tool} takes them in front of the name of the tool it runs too, and so does its deprecated name
   *       {@code run-command}, which is kept as it is typed;</li>
   *   <li>a script of the application runs in the environment of the build and reads {@code -Dgrails.env} among the
   *       arguments after its name, where it is given.</li>
   * </ul>
   * Which of these a command is, {@code document} says. A name it does not list is no command of the command line
   * itself, and is run with {@code run-tool}, which runs a tool the build has come to know since and refuses
   * every other name. An environment the command names itself where it takes one comes before the one in front of
   * it. A command without environment or system properties in front of it is run as it is typed, and
   * {@code document} is not asked.
   *
   * @param document what {@code help --json} says of the commands of the project, asked only when it is needed
   * @throws ExecutionException when an environment is named in front of a command and {@code document} lists no
   *                            command at all: where the command takes the environment is not known then, and a guess
   *                            could run a command of the application in {@code development}
   */
  public static @NotNull List<String> programArguments(@NotNull MvcCommand command,
                                                       @NotNull List<String> globalOptions,
                                                       @NotNull Supplier<Grails9HelpDocument> document) throws ExecutionException {
    List<String> arguments = new ArrayList<>(globalOptions);
    String name = command.getCommand();
    List<String> properties = command.getProperties();
    List<String> commandArguments = command.getArgs();
    String environment = command.getEnv() == null || namesEnvironment(properties) ? null
                                                                                 : ENVIRONMENT_OPTION + ENVIRONMENTS.getOrDefault(command.getEnv(), command.getEnv());
    if (name == null) {
      ContainerUtil.addIfNotNull(arguments, environment);
      arguments.addAll(properties);
      arguments.addAll(commandArguments);
      return arguments;
    }
    if (RUN_TOOL.equals(name) || RUN_COMMAND.equals(name)) {
      arguments.add(name);
      if (!namesEnvironment(optionsOfRunTool(commandArguments))) {
        ContainerUtil.addIfNotNull(arguments, environment);
      }
      arguments.addAll(properties);
      arguments.addAll(commandArguments);
      return arguments;
    }
    boolean afterTheName = true;
    if (environment != null || !properties.isEmpty()) {
      Grails9HelpDocument listed = document.get();
      if (!listed.getCommands().isEmpty()) {
        Grails9HelpDocument.Command known = listed.getCommand(name);
        afterTheName = known != null && (known.isBuiltIn() || known.isScript());
      }
      else if (environment != null || namesEnvironment(properties)) {
        String named = environment != null ? environment : ContainerUtil.findLast(properties, property -> namesEnvironment(property));
        throw new ExecutionException(GrailsBundle.message("dialog.message.grails9.environment.commands.not.listed", name, named));
      }
    }
    if (afterTheName) {
      arguments.add(name);
      if (!namesEnvironment(commandArguments)) {
        ContainerUtil.addIfNotNull(arguments, environment);
      }
      arguments.addAll(properties);
    }
    else {
      arguments.add(RUN_TOOL);
      ContainerUtil.addIfNotNull(arguments, environment);
      arguments.addAll(properties);
      arguments.add(name);
    }
    arguments.addAll(commandArguments);
    return arguments;
  }

  /**
   * @return of the arguments of {@code run-tool}, those in front of the name of the tool it runs, which are its
   * own, as the command line reads them: every argument that starts with a dash, a {@code -D} that stands alone
   * joined with the name and value after it, up to {@code --} or the first argument that is neither
   */
  private static @NotNull List<String> optionsOfRunTool(@NotNull List<String> arguments) {
    List<String> options = new ArrayList<>();
    for (int i = 0; i < arguments.size(); i++) {
      String argument = arguments.get(i);
      if (argument.equals("--")) break;
      if (argument.equals("-D") && i + 1 < arguments.size()) {
        options.add(argument + arguments.get(++i));
      }
      else if (argument.startsWith("-") && argument.length() > 1) {
        options.add(argument);
      }
      else {
        break;
      }
    }
    return options;
  }

  private static boolean namesEnvironment(@NotNull List<String> arguments) {
    for (String argument : arguments) {
      if (namesEnvironment(argument)) return true;
    }
    return false;
  }

  private static boolean namesEnvironment(@NotNull String argument) {
    return argument.equals("-Dgrails.env") || argument.startsWith(ENVIRONMENT_OPTION);
  }
}

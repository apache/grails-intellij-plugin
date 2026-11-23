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
import org.apache.grails.intellij.lib.gradle.tooling.builder.GrailsCommandLine;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.commands.Grails9CommandLines;
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
      programArguments(command, Grails9CommandLines.getInstance(grailsApplication.getProject()).globalOptions(grailsApplication)));
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
   * The arguments of the command line for {@code command}, with both options of {@link Grails9CommandLines#GLOBAL_OPTIONS}.
   */
  public static @NotNull List<String> programArguments(@NotNull MvcCommand command) {
    return programArguments(command, Grails9CommandLines.GLOBAL_OPTIONS);
  }

  /**
   * The arguments of the command line for {@code command}.
   * <p>
   * The options of the command line itself come first: {@code globalOptions}, those of {@code --plain-output} and
   * {@code --non-interactive} the command line lists as its own ({@link Grails9CommandLines#globalOptions}). A command
   * line of the Grails shell named the environment ({@code prod war}) and system properties ({@code -Dname=value
   * run-app}) in front of the command; the command line of Grails 9 refuses both there and takes them after the name of
   * the command, the environment as {@code -Dgrails.env} (upgrade note 43), so they are moved behind it.
   */
  public static @NotNull List<String> programArguments(@NotNull MvcCommand command, @NotNull List<String> globalOptions) {
    List<String> arguments = new ArrayList<>(globalOptions);
    if (command.getCommand() != null) {
      arguments.add(command.getCommand());
    }
    String environment = command.getEnv();
    if (environment != null && !namesEnvironment(command.getProperties()) && !namesEnvironment(command.getArgs())) {
      arguments.add("-Dgrails.env=" + ENVIRONMENTS.getOrDefault(environment, environment));
    }
    arguments.addAll(command.getProperties());
    arguments.addAll(command.getArgs());
    return arguments;
  }

  private static boolean namesEnvironment(@NotNull List<String> arguments) {
    for (String argument : arguments) {
      if (argument.equals("-Dgrails.env") || argument.startsWith("-Dgrails.env=")) return true;
    }
    return false;
  }
}

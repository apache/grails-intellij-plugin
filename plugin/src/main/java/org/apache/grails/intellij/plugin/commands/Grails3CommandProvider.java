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

package org.apache.grails.intellij.plugin.commands;

import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.Grails9Projects;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * The commands of the Grails shell, for a project of Grails 3 to 8. A project of Grails 9 has no shell, and its
 * commands are those its command line lists ({@link Grails9CommandProvider}); only a build of Grails 9 from before that
 * command line, or one for which the shell is chosen, runs its commands in the shell ({@link Grails9CommandLines}).
 */
final class Grails3CommandProvider implements GrailsCommandProvider {
  private static final List<String> GRAILS3_COMMANDS = List.of(
    "bug-report",
    "clean",
    "compile",
    "console",
    "create-controller",
    "create-domain-class",
    "create-functional-test",
    "create-integration-test",
    "create-interceptor",
    "create-scaffold-controller",
    "create-script",
    "create-service",
    "create-taglib",
    "create-unit-test",
    "dependency-report",
    "generate-all",
    "generate-async-controller",
    "generate-controller",
    "gradle",
    "help",
    "install",
    "install-templates",
    "list-plugins",
    "open",
    "package",
    "plugin-info",
    "run-app",
    "schema-export",
    "shell",
    "stats",
    "stop-app",
    "test-app",
    "url-mappings-report",
    "war"
  );

  @Override
  public @NotNull Collection<String> addCommands(@NotNull GrailsApplication application) {
    if (!(application instanceof Grails3Application)) return Collections.emptyList();
    if (!Grails9Projects.isGrails9OrLater(application)) return GRAILS3_COMMANDS;
    return Grails9CommandLines.getInstance(application.getProject()).awaitRunnerOf(application) == Grails9CommandLines.Runner.SHELL
           ? GRAILS3_COMMANDS
           : Collections.emptyList();
  }
}

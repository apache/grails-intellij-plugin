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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * What {@code grails help --json} says of the commands of a directory and the tools of its project, for a project of Grails 9 and later.
 * <p>
 * The document is described in "Help" of the chapter "The Command Line" of the Grails guide. This reads the members
 * the plugin uses and ignores the others, as the document asks of a reader; a document of another {@code format}
 * than {@value #FORMAT} is refused, since its format changes only where a reader of the earlier one would misread it.
 */
public final class Grails9HelpDocument {

  /** The version of the document this reads. */
  public static final int FORMAT = 1;

  /** The document of a directory whose commands are not known. */
  public static final Grails9HelpDocument EMPTY = new Grails9HelpDocument(false, List.of(), null, null, List.of());

  /** The document is one of another {@code format} than {@value #FORMAT}, which a reader of that one would misread. */
  public static final class OtherFormatException extends IOException {
    private final @NotNull String myFormat;

    OtherFormatException(@NotNull String format) {
      super("The output of help --json is a document of format " + format + "; this plugin reads format " + FORMAT);
      myFormat = format;
    }

    /** @return the {@code format} of the document, as it is written there */
    public @NotNull String getFormat() {
      return myFormat;
    }
  }

  private static final ObjectMapper MAPPER = new ObjectMapper();

  /**
   * One command of the document: a command of {@code grails} itself, or a tool the application or a plugin adds.
   * The members of a tool that an earlier build of Grails 9 does not write have the defaults named below.
   *
   * @param name        what is typed
   * @param aliases     other names of the command, such as {@code war} of {@code assemble}
   * @param description one line of plain text
   * @param origin      {@code built-in} for a command of the command line itself, otherwise {@code application},
   *                    the coordinates of the plugin artifact it comes from, or a jar
   * @param target      of a tool, {@code application} for one the build runs and {@code standalone} for one the command
   *                    line starts as a process of its own; empty for a command of the command line itself, and for
   *                    a build that does not say. A value other than those two is kept as it is
   * @param startsApplication whether the build starts the application around the tool; {@code false} for a command of
   *                    the command line itself, for a standalone tool and for a script. Where the document has no such
   *                    member, {@code true} for a tool that is neither a standalone tool nor a script
   * @param registration how the build of the project came to list a tool of the application or of a plugin,
   *                     {@code script} for a script of the application; empty for a command of the command line itself
   * @param deprecated  of a tool, what of it is deprecated: {@code type} for a class of a deprecated type and
   *                    {@code folder} for a tool in the deprecated {@code grails-app/commands}; empty where nothing is
   * @param deprecatedNames the names of a command of the command line itself that are deprecated, as {@code run-command}
   *                    of {@code run-tool}; empty where it has none
   * @param project     whether the command works on a project and cannot be used outside one
   * @param commands    the commands of the command, such as {@code add} of {@code feature}
   */
  public record Command(@NotNull String name,
                        @NotNull @Unmodifiable List<String> aliases,
                        @NotNull String description,
                        @NotNull String origin,
                        @NotNull String target,
                        boolean startsApplication,
                        @NotNull String registration,
                        @NotNull @Unmodifiable List<String> deprecated,
                        @NotNull @Unmodifiable List<String> deprecatedNames,
                        boolean project,
                        @NotNull @Unmodifiable List<Command> commands) {

    /** The {@link #origin()} of a command of the command line itself. */
    public static final String BUILT_IN = "built-in";

    /** The {@link #registration()} of a script of the application. */
    public static final String SCRIPT = "script";

    /** The {@link #target()} of a tool the build runs. */
    public static final String TARGET_APPLICATION = "application";

    /** The {@link #target()} of a tool the command line starts as a process of its own. */
    public static final String TARGET_STANDALONE = "standalone";

    /** @return whether this is a tool the command line starts as a process of its own, without Gradle */
    public boolean isStandalone() {
      return TARGET_STANDALONE.equals(target);
    }

    /** @return whether something of this tool is deprecated: its type, or the folder it is in */
    public boolean isDeprecated() {
      return !deprecated.isEmpty();
    }

    /** @return whether {@code name} is a deprecated name of this command, which still works */
    public boolean isDeprecatedName(@NotNull String name) {
      return deprecatedNames.contains(name);
    }

    /** @return whether this is a command of the command line itself, as {@code war} is, and not one the project adds */
    public boolean isBuiltIn() {
      return BUILT_IN.equals(origin);
    }

    /** @return whether this is a script of the application, which the build runs without the application */
    public boolean isScript() {
      return SCRIPT.equals(registration);
    }
  }

  private final boolean myComplete;
  private final @NotNull @Unmodifiable List<String> myDiagnostics;
  private final @Nullable String myProjectDirectory;
  private final @Nullable @Unmodifiable List<String> myOptionNames;
  private final @NotNull @Unmodifiable List<Command> myCommands;

  private Grails9HelpDocument(boolean complete,
                              @NotNull List<String> diagnostics,
                              @Nullable String projectDirectory,
                              @Nullable List<String> optionNames,
                              @NotNull List<Command> commands) {
    myComplete = complete;
    myDiagnostics = List.copyOf(diagnostics);
    myProjectDirectory = projectDirectory;
    myOptionNames = optionNames == null ? null : List.copyOf(optionNames);
    myCommands = List.copyOf(commands);
  }

  /**
   * @param json the standard output of {@code grails help --json}
   * @throws OtherFormatException when it is a JSON document whose {@code format} is a number other than {@value #FORMAT}
   * @throws IOException          when it is no JSON document of format {@value #FORMAT}
   */
  public static @NotNull Grails9HelpDocument parse(@NotNull String json) throws IOException {
    JsonNode root = MAPPER.readTree(json);
    if (root == null || !root.isObject()) {
      throw new IOException("The output of help --json is no JSON object");
    }
    JsonNode format = root.get("format");
    if (format == null || !format.isIntegralNumber()) {
      throw new IOException("The output of help --json has no format: " + format);
    }
    if (!format.canConvertToInt() || format.intValue() != FORMAT) {
      throw new OtherFormatException(format.asText());
    }
    List<String> diagnostics = new ArrayList<>();
    for (JsonNode diagnostic : root.path("diagnostics")) {
      if (diagnostic.isTextual()) diagnostics.add(diagnostic.textValue());
    }
    JsonNode projectDirectory = root.get("projectDirectory");
    JsonNode options = root.get("options");
    return new Grails9HelpDocument(root.path("complete").asBoolean(false),
                                   diagnostics,
                                   projectDirectory != null && projectDirectory.isTextual() ? projectDirectory.textValue() : null,
                                   options != null && options.isArray() ? optionNames(options) : null,
                                   commands(root.path("commands")));
  }

  private static @NotNull List<String> optionNames(@NotNull JsonNode options) {
    List<String> result = new ArrayList<>();
    for (JsonNode option : options) {
      for (JsonNode name : option.path("names")) {
        if (name.isTextual() && !name.textValue().isBlank()) result.add(name.textValue());
      }
    }
    return result;
  }

  private static @NotNull List<Command> commands(@NotNull JsonNode array) {
    List<Command> result = new ArrayList<>();
    for (JsonNode command : array) {
      JsonNode name = command.get("name");
      if (name == null || !name.isTextual() || name.textValue().isBlank()) continue;
      List<String> aliases = strings(command.path("aliases"));
      String registration = command.path("registration").asText("");
      String target = command.path("target").asText("");
      JsonNode starts = command.get("startsApplication");
      boolean startsApplication = starts != null && starts.isBoolean()
                                  ? starts.booleanValue()
                                  : !Command.TARGET_STANDALONE.equals(target) && !Command.SCRIPT.equals(registration)
                                    && !Command.BUILT_IN.equals(command.path("origin").asText(""));
      result.add(new Command(name.textValue(),
                             List.copyOf(aliases),
                             command.path("description").asText(""),
                             command.path("origin").asText(""),
                             target,
                             startsApplication,
                             registration,
                             strings(command.path("deprecated")),
                             strings(command.path("deprecatedNames")),
                             command.path("project").asBoolean(false),
                             List.copyOf(commands(command.path("commands")))));
    }
    return result;
  }

  private static @NotNull List<String> strings(@NotNull JsonNode array) {
    List<String> result = new ArrayList<>();
    for (JsonNode element : array) {
      if (element.isTextual() && !element.textValue().isBlank()) result.add(element.textValue());
    }
    return List.copyOf(result);
  }

  /** @return {@code false} when not every command of the directory could be found; {@link #getDiagnostics()} says why */
  public boolean isComplete() {
    return myComplete;
  }

  public @NotNull @Unmodifiable List<String> getDiagnostics() {
    return myDiagnostics;
  }

  /** @return the directory of the project the command ran in, or {@code null} outside a project */
  public @Nullable String getProjectDirectory() {
    return myProjectDirectory;
  }

  /**
   * @return every name of the options the command line itself takes in front of a command (the {@code options} of the
   * document), such as {@code --plain-output}; {@code null} when the document has no such member
   */
  public @Nullable @Unmodifiable List<String> getOptionNames() {
    return myOptionNames;
  }

  /** @return the commands, in the order {@code help} lists them */
  public @NotNull @Unmodifiable List<Command> getCommands() {
    return myCommands;
  }

  /** @return the command that is typed as {@code name}, its name or one of its aliases, or {@code null} when none is listed */
  public @Nullable Command getCommand(@NotNull String name) {
    for (Command command : myCommands) {
      if (command.name().equals(name) || command.aliases().contains(name)) return command;
    }
    return null;
  }

  /**
   * @return what can be offered as the first word of a command: every name and alias, in the order of the help, except
   * the aliases that are deprecated names ({@code run-command}), which still work and are not offered
   */
  public @NotNull @Unmodifiable List<String> getCommandNames() {
    Set<String> names = new LinkedHashSet<>();
    for (Command command : myCommands) {
      names.add(command.name());
      for (String alias : command.aliases()) {
        if (!command.isDeprecatedName(alias)) names.add(alias);
      }
    }
    return List.copyOf(names);
  }
}

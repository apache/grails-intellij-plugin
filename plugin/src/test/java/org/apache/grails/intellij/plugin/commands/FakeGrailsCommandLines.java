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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * Writes stand-ins for the jar of {@code org.apache.grails:grails-cli} into a directory: real jars, started by
 * {@link FakeGrailsCommandLine}, that answer {@code help --json} as a test needs, and count how often they are started.
 */
public final class FakeGrailsCommandLines {

  /** A document of the format the plugin reads, which lists the two options the plugin gives and no command. */
  public static final String FORMAT_1 = """
    {"format":1,"complete":true,"diagnostics":[],"projectDirectory":null,"options":[
    {"names":["--non-interactive"]},{"names":["--plain-output"]},{"names":["-V","--version"]}],"commands":[
    {"name":"create-controller","aliases":[],"description":"Creates a controller","project":true,"origin":"built-in","commands":[]}]}
    """;

  /** A document of a format after the one the plugin reads. */
  public static final String FORMAT_2 = "{\"format\":2,\"commandsByName\":{}}";

  private final @NotNull Path myDirectory;

  public FakeGrailsCommandLines(@NotNull Path directory) {
    myDirectory = directory;
  }

  /** @return a jar that answers {@code help --json} at once with {@code document} */
  public @NotNull Path answering(@NotNull String name, @NotNull String document) throws IOException {
    return answeringAfter(name, document, 0);
  }

  /** @return a jar that answers {@code help --json} with {@code document} after {@code millis} */
  public @NotNull Path answeringAfter(@NotNull String name, @NotNull String document, long millis) throws IOException {
    Properties behaviour = new Properties();
    behaviour.setProperty("stdout", "help.json");
    behaviour.setProperty("sleepMillis", Long.toString(millis));
    return jar(name, behaviour, Map.of("help.json", document));
  }

  /**
   * @return a stand-in for the jar of a build of Grails 9 from before its command line: with {@code GRAILS_PREFERRED_SHELL=forge}
   * it refuses the options of the command line at once, with exit code 2, as the Grails Forge CLI of that jar does;
   * without it, it takes two minutes and ends with exit code 1, as the Grails shell there does
   */
  public @NotNull Path beforeTheCommandLine(@NotNull String name) throws IOException {
    Properties behaviour = new Properties();
    behaviour.setProperty("forgeExitCode", "2");
    behaviour.setProperty("sleepMillis", "120000");
    behaviour.setProperty("stderr", "Error occurred running Grails CLI");
    behaviour.setProperty("exitCode", "1");
    return jar(name, behaviour, Map.of());
  }

  /** @return a jar that does not end for two minutes */
  public @NotNull Path hanging(@NotNull String name) throws IOException {
    Properties behaviour = new Properties();
    behaviour.setProperty("sleepMillis", "120000");
    return jar(name, behaviour, Map.of());
  }

  /** @return every time {@code jar} was started, one line each: its arguments and the {@code GRAILS_PREFERRED_SHELL} it had */
  public static @NotNull List<String> starts(@NotNull Path jar) throws IOException {
    Path record = record(jar);
    return Files.exists(record) ? Files.readAllLines(record) : List.of();
  }

  private static @NotNull Path record(@NotNull Path jar) {
    return jar.resolveSibling(jar.getFileName() + ".starts");
  }

  private @NotNull Path jar(@NotNull String name, @NotNull Properties behaviour, @NotNull Map<String, String> entries) throws IOException {
    Path jar = myDirectory.resolve(name);
    behaviour.setProperty("record", record(jar).toString());
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, FakeGrailsCommandLine.class.getName());
    Map<String, byte[]> content = new LinkedHashMap<>();
    content.put(FakeGrailsCommandLine.class.getName().replace('.', '/') + ".class", startClass());
    content.put(FakeGrailsCommandLine.BEHAVIOUR, properties(behaviour));
    entries.forEach((entry, text) -> content.put(entry, text.getBytes(StandardCharsets.UTF_8)));
    try (OutputStream file = Files.newOutputStream(jar); JarOutputStream out = new JarOutputStream(file, manifest)) {
      for (Map.Entry<String, byte[]> entry : content.entrySet()) {
        out.putNextEntry(new JarEntry(entry.getKey()));
        out.write(entry.getValue());
        out.closeEntry();
      }
    }
    return jar;
  }

  private static byte[] startClass() throws IOException {
    try (InputStream in = FakeGrailsCommandLine.class.getResourceAsStream(FakeGrailsCommandLine.class.getSimpleName() + ".class")) {
      if (in == null) throw new IOException("The class file of " + FakeGrailsCommandLine.class.getName() + " is not on the class path");
      return in.readAllBytes();
    }
  }

  private static byte[] properties(@NotNull Properties behaviour) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    behaviour.store(out, null);
    return out.toByteArray();
  }
}

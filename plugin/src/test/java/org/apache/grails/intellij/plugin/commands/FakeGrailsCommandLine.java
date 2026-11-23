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

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Properties;

/**
 * The start class of the stand-ins for the jar of {@code org.apache.grails:grails-cli} that
 * {@link FakeGrailsCommandLines} writes, which the tests start as a real process, as the plugin starts the jar. It uses
 * nothing but the JDK, and does what {@value #BEHAVIOUR} in its jar says:
 * <ul>
 *   <li>{@code record}: a file it adds one line to whenever it is started, with its arguments and the value of
 *       {@code GRAILS_PREFERRED_SHELL};</li>
 *   <li>{@code forgeExitCode}: when {@code GRAILS_PREFERRED_SHELL} is {@code forge}, the exit code it ends with at once,
 *       after the line the Grails Forge CLI of a build of Grails 9 from before its command line writes;</li>
 *   <li>{@code sleepMillis}: how long it takes before it answers;</li>
 *   <li>{@code stdout}: the entry of the jar it writes on the standard output;</li>
 *   <li>{@code stderr}: a line it writes on the standard error;</li>
 *   <li>{@code exitCode}: the exit code it then ends with.</li>
 * </ul>
 */
public final class FakeGrailsCommandLine {

  static final String BEHAVIOUR = "fake-grails-cli.properties";

  private FakeGrailsCommandLine() {
  }

  public static void main(String[] args) throws IOException, InterruptedException {
    Properties behaviour = new Properties();
    try (InputStream in = FakeGrailsCommandLine.class.getResourceAsStream("/" + BEHAVIOUR)) {
      behaviour.load(in);
    }
    String preferredShell = System.getenv("GRAILS_PREFERRED_SHELL");
    String record = behaviour.getProperty("record");
    if (record != null) {
      Files.writeString(Path.of(record), String.join(" ", args) + " | GRAILS_PREFERRED_SHELL=" + preferredShell + "\n",
                        StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }
    String forgeExitCode = behaviour.getProperty("forgeExitCode");
    if (forgeExitCode != null && "forge".equals(preferredShell)) {
      System.err.println("Unknown options: '--plain-output', '--non-interactive', 'help', '--json'");
      System.exit(Integer.parseInt(forgeExitCode));
    }
    Thread.sleep(Long.parseLong(behaviour.getProperty("sleepMillis", "0")));
    String stdout = behaviour.getProperty("stdout");
    if (stdout != null) {
      try (InputStream in = FakeGrailsCommandLine.class.getResourceAsStream("/" + stdout)) {
        System.out.write(in.readAllBytes());
      }
      System.out.flush();
    }
    String stderr = behaviour.getProperty("stderr");
    if (stderr != null) {
      System.err.println(stderr);
    }
    System.exit(Integer.parseInt(behaviour.getProperty("exitCode", "0")));
  }
}

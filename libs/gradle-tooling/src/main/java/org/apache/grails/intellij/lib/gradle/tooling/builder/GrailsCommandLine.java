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

package org.apache.grails.intellij.lib.gradle.tooling.builder;

import org.jetbrains.annotations.Nullable;

/**
 * The command line of Grails 9 and later, as a tool starts it.
 * <p>
 * Until Grails 8 a project's commands ran in the Grails shell, {@code org.grails.cli.GrailsCli} of
 * {@code org.apache.grails:grails-shell-cli}. Grails 9 publishes no shell: it has one command line, the
 * artifact {@code org.apache.grails:grails-cli}, which a tool starts with {@code --plain-output --non-interactive}
 * and asks {@code help --json} for the commands of a directory (upgrade note 51 of the Grails guide). The jar
 * with the {@link #CLASSIFIER} classifier holds the command line and every class it needs, as the one the Grails
 * wrapper downloads does, so it resolves from the repositories of the project alone; its plain jar would bring
 * the Gradle Tooling API, which is not on Maven Central.
 * <p>
 * The class that starts the command line is the one the {@code Main-Class} attribute of the manifest of that
 * jar names, as the Grails wrapper of Grails 9 starts it, so that a tool need not change when the class moves;
 * {@link #FALLBACK_MAIN_CLASS} is only for a jar whose manifest names none.
 * <p>
 * Which of the two a project has is told by the major version of {@code grailsVersion}, as the command line
 * itself tells it, and not by the {@code .grails} record of the project, which an application of Grails 8
 * can have too after {@code grails project migrate}. The builds of Grails 9 from before its command line (snapshots
 * and milestones) still have the shell: for a project of Grails 9 and later the import resolves the shell of the
 * version too ({@link #shellCoordinates(String)}), and the IDE uses it when the jar of {@code grails-cli} does not
 * answer {@code help --json} as the command line does.
 */
public final class GrailsCommandLine {

  public static final String GROUP = "org.apache.grails";

  public static final String ARTIFACT = "grails-cli";

  public static final String CLASSIFIER = "all";

  /** The artifact of the Grails shell of Grails 7 and 8, which the builds of Grails 9 from before its command line still publish. */
  public static final String SHELL_ARTIFACT = "grails-shell-cli";

  /** The class that starts the command line of Grails 9, for a jar whose manifest names no {@code Main-Class}. */
  public static final String FALLBACK_MAIN_CLASS = "org.apache.grails.forge.cli.GrailsCli";

  /** The first major version of Grails whose projects use this command line. */
  public static final int FIRST_MAJOR_VERSION = 9;

  private GrailsCommandLine() {
  }

  /**
   * @param grailsVersion a version of Grails, such as {@code 9.0.0}, {@code 9.0.0-SNAPSHOT} or {@code 8.1.2}
   * @return whether a project on that version uses the command line of Grails 9 and later; {@code false}
   * for a version that does not start with a number
   */
  public static boolean isUsedBy(@Nullable String grailsVersion) {
    int major = majorVersion(grailsVersion);
    return major >= FIRST_MAJOR_VERSION;
  }

  /**
   * @return the coordinates of the jar that holds the command line of {@code grailsVersion}, for a
   * dependency declaration ({@code group:artifact:version:classifier})
   */
  public static String coordinates(String grailsVersion) {
    return GROUP + ":" + ARTIFACT + ":" + grailsVersion + ":" + CLASSIFIER;
  }

  /**
   * @return the coordinates of the Grails shell of {@code grailsVersion} ({@code group:artifact:version})
   */
  public static String shellCoordinates(String grailsVersion) {
    return GROUP + ":" + SHELL_ARTIFACT + ":" + grailsVersion;
  }

  private static int majorVersion(@Nullable String version) {
    if (version == null) return -1;
    String trimmed = version.trim();
    int end = 0;
    while (end < trimmed.length() && Character.isDigit(trimmed.charAt(end))) {
      end++;
    }
    if (end == 0 || end > 9) return -1;
    return Integer.parseInt(trimmed.substring(0, end));
  }
}

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

package org.apache.grails.intellij.plugin.structure;

import com.intellij.openapi.vfs.VirtualFile;
import org.apache.grails.intellij.lib.gradle.tooling.builder.GrailsCommandLine;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Tells the projects of Grails 9 and later from those of earlier versions.
 * <p>
 * A project of Grails 9 has no Grails shell: its commands run in the one command line of Grails 9
 * ({@link GrailsCommandLine}), and its build commands are Gradle tasks. What decides it is the version the
 * project is on, {@code grailsVersion} in {@code gradle.properties}, which the Gradle import records; that
 * is also how the command line of Grails 9 decides whether it works with a project.
 * <p>
 * A project that Grails 9 generated also has a record, {@code .grails/project.yml}
 * (see {@link #hasProjectRecord(VirtualFile)}). The record marks the root of a Grails project, as
 * {@code grails-app} does, but does not make it a project of Grails 9: {@code grails project migrate} writes
 * it for an application that is still on Grails 7 or 8.
 */
public final class Grails9Projects {

  /** The directory of the record of a project that Grails 9 generated. */
  public static final String RECORD_DIRECTORY = ".grails";

  /** The file of that record that names the type, package and features of the project. */
  public static final String RECORD_FILE = "project.yml";

  private Grails9Projects() {
  }

  /**
   * @return whether the application is on Grails 9 or later, by the {@code grailsVersion} of its Gradle model
   */
  @Contract("null -> false")
  public static boolean isGrails9OrLater(@Nullable GrailsApplication application) {
    return application instanceof Grails3Application grails3
           && GrailsCommandLine.isUsedBy(grails3.getGradleData().getGrailsVersion());
  }

  /**
   * @return whether {@code root} has the record that Grails 9 writes for a project, {@code .grails/project.yml}
   */
  public static boolean hasProjectRecord(@NotNull VirtualFile root) {
    return findProjectRecord(root) != null;
  }

  /**
   * @return the {@code .grails/project.yml} of the project at {@code root}, or {@code null} when there is no such file
   */
  public static @Nullable VirtualFile findProjectRecord(@NotNull VirtualFile root) {
    VirtualFile directory = root.findChild(RECORD_DIRECTORY);
    if (directory == null || !directory.isDirectory()) return null;
    VirtualFile record = directory.findChild(RECORD_FILE);
    return record != null && !record.isDirectory() ? record : null;
  }

  /**
   * @param record a {@code project.yml} file
   * @return the root of the project whose record it is, or {@code null} when it is not a record of a project
   */
  public static @Nullable VirtualFile projectRootOfRecord(@NotNull VirtualFile record) {
    if (record.isDirectory() || !RECORD_FILE.equals(record.getName())) return null;
    VirtualFile directory = record.getParent();
    if (directory == null || !RECORD_DIRECTORY.equals(directory.getName())) return null;
    return directory.getParent();
  }
}

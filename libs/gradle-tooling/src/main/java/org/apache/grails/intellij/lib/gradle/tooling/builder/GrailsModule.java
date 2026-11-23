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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;
import java.util.List;

public interface GrailsModule extends Serializable {

  @NotNull
  String getGrailsVersion();

  @NotNull
  String getGrailsPluginId();

  /**
   * @return the class path of the Grails shell of the project's version, which runs its commands up to Grails 8;
   * {@code null} when it could not be resolved. For a project of Grails 9 and later it is there only when that
   * version still publishes the shell, as the builds of Grails 9 from before its command line do.
   */
  @Nullable
  List<String> getShellUrls();

  /**
   * @return for a project of Grails 9 and later, the jar of its command line ({@link GrailsCommandLine});
   * {@code null} for an earlier version and when it could not be resolved
   */
  @Nullable
  List<String> getCommandLineUrls();
}

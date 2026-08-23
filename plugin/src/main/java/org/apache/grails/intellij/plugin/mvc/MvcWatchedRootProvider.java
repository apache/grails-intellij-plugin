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
package org.apache.grails.intellij.plugin.mvc;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.WatchedRootsProvider;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.config.GrailsFramework;

import java.io.File;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class MvcWatchedRootProvider implements WatchedRootsProvider {
  @Override
  public @NotNull Set<String> getRootsToWatch(@NotNull Project project) {
    return doGetRootsToWatch(project);
  }

  public static @NotNull Set<String> doGetRootsToWatch(@NotNull Project project) {
    if (!project.isInitialized()) {
      return Collections.emptySet();
    }

    Set<String> result = null;

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      GrailsFramework framework = GrailsFramework.getInstance(module);
      if (framework == null) {
        continue;
      }

      if (result == null) {
        result = new HashSet<>();
      }

      File sdkWorkDir = framework.getCommonPluginsDir(module);
      if (sdkWorkDir != null) {
        result.add(sdkWorkDir.getAbsolutePath());
      }

      File globalPluginsDir = framework.getGlobalPluginsDir(module);
      if (globalPluginsDir != null) {
        result.add(globalPluginsDir.getAbsolutePath());
      }
    }

    return result == null ? Collections.emptySet() : result;
  }
}

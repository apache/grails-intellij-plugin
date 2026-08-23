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
package org.apache.grails.intellij.plugin.config;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vcs.FilePath;
import com.intellij.openapi.vcs.changes.IgnoredBeanFactory;
import com.intellij.openapi.vcs.changes.IgnoredFileDescriptor;
import com.intellij.openapi.vcs.changes.IgnoredFileProvider;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;

import java.util.Collections;
import java.util.Set;

/** Keeps the shared {@code ~/.grails} cache out of VCS when it happens to sit inside the project. */
public final class GrailsIgnoredProvider implements IgnoredFileProvider {

  @Override
  public boolean isIgnoredFile(@NotNull Project project, @NotNull FilePath filePath) {
    return FileUtil.isAncestor(GrailsFramework.getUserHomeGrails(), filePath.getPath(), false);
  }

  @Override
  public @NotNull Set<IgnoredFileDescriptor> getIgnoredFiles(@NotNull Project project) {
    String grailsDir = GrailsFramework.getUserHomeGrails();
    String projectBasePath = project.getBasePath();
    if (projectBasePath == null) return Collections.emptySet();

    if (FileUtil.isAncestor(projectBasePath, grailsDir, true)) {
      return Set.of(IgnoredBeanFactory.ignoreUnderDirectory(grailsDir, project));
    }

    return Collections.emptySet();
  }

  @Override
  public @NotNull String getIgnoredGroupDescription() {
    return GrailsBundle.message("ignored.files.description.framework.dir");
  }
}

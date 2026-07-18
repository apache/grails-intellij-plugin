/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.intellij.groovy.grails.maven;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.idea.maven.project.MavenProjectsManager;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationProvider;

final class GrailsMavenApplicationProvider extends GrailsApplicationProvider {

  @Override
  public @Nullable GrailsApplication createApplication(@NotNull Project project, @NotNull VirtualFile root) {
    final Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(root);
    if (module == null) return null;
    final MavenProject mavenProject = MavenProjectsManager.getInstance(project).findProject(module);
    if (mavenProject == null
        || !mavenProject.getDirectoryFile().equals(root)
        || mavenProject.findPlugin("org.grails", "grails-maven-plugin") == null) {
      return null;
    }
    return new GrailsMavenApplication(module, root, mavenProject.getMavenId());
  }
}

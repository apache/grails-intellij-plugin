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
package org.apache.grails.intellij.plugin.structure.impl;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.apache.grails.intellij.plugin.gradle.GrailsModuleData;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationProvider;

public final class Grails3ApplicationProvider extends GrailsApplicationProvider {

  @Override
  public @Nullable GrailsApplication createApplication(@NotNull Project project, @NotNull VirtualFile root) {
    String path = root.getPath();

    ExternalProjectSettings projectSettings =
      ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).getLinkedProjectSettings(path);
    if (!(projectSettings instanceof GradleProjectSettings linkedProjectSettings)) return null;

    ExternalProjectInfo gradleProjectInfo = ProjectDataManager.getInstance()
      .getExternalProjectData(project, GradleConstants.SYSTEM_ID, linkedProjectSettings.getExternalProjectPath());
    if (gradleProjectInfo == null) return null;

    DataNode<ProjectData> structure = gradleProjectInfo.getExternalProjectStructure();
    DataNode<ModuleData> moduleData = GradleProjectResolverUtil.findModule(structure, path);
    if (moduleData == null) return null;
    if (ExternalSystemApiUtil.find(moduleData, GrailsModuleData.KEY) == null) return null;

    if (linkedProjectSettings.isResolveModulePerSourceSet()) {
      return new Grails3MultiModuleApplication(project, root, moduleData);
    }

    Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(root);
    return module != null ? new Grails3SingleModuleApplication(module, root, moduleData) : null;
  }
}

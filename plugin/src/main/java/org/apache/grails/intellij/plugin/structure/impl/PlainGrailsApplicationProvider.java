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

package org.apache.grails.intellij.plugin.structure.impl;

import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationProvider;

public final class PlainGrailsApplicationProvider extends GrailsApplicationProvider {

  @Override
  public @Nullable GrailsApplication createApplication(@NotNull Project project, @NotNull VirtualFile root) {
    final Module module = ContainerUtil.find(
      ModuleManager.getInstance(project).getModules(),
      m -> ContainerUtil.find(
        ModuleRootManager.getInstance(m).getContentRoots(),
        cc -> cc.equals(root)
      ) != null
    );
    if (module == null) return null;
    if (GrailsFramework.getInstance().isAuxModule(module)) return null;
    if (ExternalSystemApiUtil.isExternalSystemAwareModule(GradleConstants.SYSTEM_ID, module)) return null;
    if (VfsUtil.findRelativeFile(root, "application.properties") == null) return null;
    if (VfsUtil.findRelativeFile(root, "grails-app", "conf", "BuildConfig.groovy") == null) return null;
    return new Grails2Application(root, module);
  }
}

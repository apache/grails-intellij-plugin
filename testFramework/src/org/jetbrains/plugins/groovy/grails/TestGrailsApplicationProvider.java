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

package org.jetbrains.plugins.groovy.grails;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationProvider;
import org.jetbrains.plugins.grails.structure.impl.Grails2Application;

public class TestGrailsApplicationProvider extends GrailsApplicationProvider {

  @Nullable
  @Override
  public GrailsApplication createApplication(@NotNull Project project, @NotNull VirtualFile root) {
    final Module module = ProjectRootManager.getInstance(project).getFileIndex().getModuleForFile(root);
    if (module == null) return null;
    if (GrailsFramework.getInstance().isAuxModule(module)) return null;
    if (VfsUtil.findRelativeFile(root, "application.properties") == null) return null;
    if (VfsUtil.findRelativeFile(root, "plugin.xml") != null) return null;
    return new Grails2Application(root, module);
  }
}

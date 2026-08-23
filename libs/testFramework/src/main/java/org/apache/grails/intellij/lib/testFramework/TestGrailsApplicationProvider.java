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

package org.apache.grails.intellij.lib.testFramework;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationProvider;
import org.apache.grails.intellij.plugin.structure.impl.Grails2Application;

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

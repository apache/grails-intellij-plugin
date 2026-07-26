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
package org.jetbrains.plugins.grails.structure.impl;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.model.data.GradleSourceSetData;

import java.util.ArrayList;
import java.util.List;

final class Grails3MultiModuleApplication extends Grails3ApplicationBase {

  private final DataNode<ModuleData> myModuleDataNode;

  // The source sets come from the external-system model and do not change for a given node.
  private volatile List<GradleSourceSetData> mySourceSets;

  Grails3MultiModuleApplication(@NotNull Project project,
                                @NotNull VirtualFile root,
                                @NotNull DataNode<ModuleData> moduleDataNode) {
    super(project, root, moduleDataNode);
    myModuleDataNode = moduleDataNode;
  }

  @Override
  public @NotNull GlobalSearchScope getScope(boolean includeDependencies, boolean testsOnly) {
    if (testsOnly) {
      return sourceSetScope("test", includeDependencies, true)
        .uniteWith(sourceSetScope("integrationTest", includeDependencies, true));
    }
    return sourceSetScope("main", includeDependencies, false);
  }

  private @NotNull GlobalSearchScope sourceSetScope(@NotNull String sourceSetName,
                                                    boolean includeDependencies,
                                                    boolean testsOnly) {
    Module module = findModule(sourceSetName);
    if (module == null) return GlobalSearchScope.EMPTY_SCOPE;
    return includeDependencies ? module.getModuleRuntimeScope(testsOnly) : module.getModuleScope(testsOnly);
  }

  private @NotNull List<GradleSourceSetData> getSourceSets() {
    List<GradleSourceSetData> result = mySourceSets;
    if (result == null) {
      result = new ArrayList<>();
      for (DataNode<GradleSourceSetData> node : ExternalSystemApiUtil.findAll(myModuleDataNode, GradleSourceSetData.KEY)) {
        result.add(node.getData());
      }
      mySourceSets = result;
    }
    return result;
  }

  private @Nullable Module findModule(@NotNull String sourceSetName) {
    GradleSourceSetData sourceSet = null;
    for (GradleSourceSetData candidate : getSourceSets()) {
      if (candidate.getId().endsWith(":" + sourceSetName)) {
        sourceSet = candidate;
        break;
      }
    }
    if (sourceSet == null) return null;

    for (Module module : ModuleManager.getInstance(getProject()).getModules()) {
      if (sourceSet.getId().equals(ExternalSystemApiUtil.getExternalProjectId(module))) {
        return module;
      }
    }
    return null;
  }
}

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
package org.jetbrains.plugins.grails.gradle;

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.Key;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.service.project.IdeModelsProvider;
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProvider;
import com.intellij.openapi.externalSystem.service.project.manage.AbstractProjectDataService;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;

import java.util.Collection;

import static com.intellij.execution.runners.ExecutionUtil.PROPERTY_DYNAMIC_CLASSPATH;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public final class GrailsModuleGradleDataService extends AbstractProjectDataService<GrailsModuleData, Module> {

  @Override
  public @NotNull Key<GrailsModuleData> getTargetDataKey() {
    return GrailsModuleData.KEY;
  }

  @Override
  public void importData(final @NotNull Collection<? extends DataNode<GrailsModuleData>> toImport,
                         final @Nullable ProjectData projectData,
                         final @NotNull Project project,
                         final @NotNull IdeModifiableModelsProvider modelsProvider) {
    if (toImport.isEmpty() || !project.isInitialized()) {
      return;
    }

    if (SystemInfo.isWindows) {
      PropertiesComponent.getInstance(project).setValue(PROPERTY_DYNAMIC_CLASSPATH, "true");
    }
  }

  @Override
  public void onSuccessImport(@NotNull Collection<DataNode<GrailsModuleData>> imported,
                              @Nullable ProjectData projectData,
                              @NotNull Project project,
                              @NotNull IdeModelsProvider modelsProvider) {
    GrailsApplicationManager.getInstance(project).queueUpdate();
  }
}

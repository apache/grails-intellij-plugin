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

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.config.GrailsModuleStructureUtil;
import org.apache.grails.intellij.plugin.config.GrailsSettingsService;
import org.apache.grails.intellij.plugin.config.PrintGrailsSettingsConstants;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.apache.grails.intellij.plugin.util.version.VersionImpl;

// Public because it is referenced from the runner package. It was Kotlin `internal`, which is
// public in bytecode, so cross-package use compiled; package-private here would not.
@ApiStatus.Internal
public final class Grails2Application extends OldGrailsModuleBasedApplication {

  public Grails2Application(@NotNull VirtualFile root, @NotNull Module module) {
    super(module, root);
  }

  @Override
  public @NotNull String getName() {
    String name = getApplicationPropertiesValue("app.name");
    return name != null ? name : getModule().getName();
  }

  @Override
  public @Nullable String getAppVersion() {
    return getApplicationPropertiesValue("app.version");
  }

  public @Nullable Version getApplicationPropertiesVersion() {
    String version = getApplicationPropertiesValue(GrailsModuleStructureUtil.GRAILS_VERSION_KEY);
    return version == null || version.isBlank() ? null : new VersionImpl(version);
  }

  public boolean isRunForked() {
    return getBooleanSetting(PrintGrailsSettingsConstants.DEBUG_RUN_FORK);
  }

  public boolean isTestForked() {
    return getBooleanSetting(PrintGrailsSettingsConstants.DEBUG_TEST_FORK);
  }

  private @Nullable String getApplicationPropertiesValue(@NotNull String key) {
    return ReadAction.compute(() -> GrailsProperties.getPropertyValue(getApplicationProperties(), key));
  }

  private boolean getBooleanSetting(@NotNull String key) {
    return ReadAction.compute(() -> {
      String value = GrailsSettingsService.getGrailsSettings(getModule()).properties.get(key);
      return value != null && Boolean.parseBoolean(value);
    });
  }
}

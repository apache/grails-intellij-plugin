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
package org.apache.grails.intellij.lib.gradle.tooling.builder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * @author Vladislav.Soroka
 */
public class GrailsModuleImpl implements GrailsModule {

  private static final long serialVersionUID = 1L;

  private final @NotNull String myGrailsVersion;

  private final @NotNull String myGrailsPluginId;

  private final @Nullable List<String> myShellUrls;

  public GrailsModuleImpl(@NotNull String grailsVersion, @NotNull String grailsPluginId) {
    this(grailsVersion, grailsPluginId, null);
  }

  public GrailsModuleImpl(@NotNull String grailsVersion, @NotNull String grailsPluginId, @Nullable List<String> urls) {
    myGrailsVersion = grailsVersion;
    myGrailsPluginId = grailsPluginId;
    myShellUrls = urls;
  }

  @Override
  public @NotNull String getGrailsVersion() {
    return myGrailsVersion;
  }

  @Override
  public @NotNull String getGrailsPluginId() {
    return myGrailsPluginId;
  }

  @Override
  public @Nullable List<String> getShellUrls() {
    return myShellUrls;
  }
}

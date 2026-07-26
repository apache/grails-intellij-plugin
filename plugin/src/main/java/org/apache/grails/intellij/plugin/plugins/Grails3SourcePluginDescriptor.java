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
package org.apache.grails.intellij.plugin.plugins;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;

/** A plugin whose source lives in another module of this project. */
public final class Grails3SourcePluginDescriptor implements GrailsPluginDescriptor {

  private final @NotNull PsiClass myPluginClass;
  private final @NotNull GrailsApplication myPluginApplication;

  public Grails3SourcePluginDescriptor(@NotNull PsiClass pluginClass, @NotNull GrailsApplication pluginApplication) {
    myPluginClass = pluginClass;
    myPluginApplication = pluginApplication;
  }

  @Override
  public @NotNull PsiClass getPluginClass() {
    return myPluginClass;
  }

  public @NotNull GrailsApplication getPluginApplication() {
    return myPluginApplication;
  }

  @Override
  public @Nullable String getPluginVersion() {
    return myPluginApplication.getAppVersion();
  }
}

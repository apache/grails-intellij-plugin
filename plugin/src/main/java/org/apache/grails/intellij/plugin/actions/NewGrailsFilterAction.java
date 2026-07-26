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

package org.apache.grails.intellij.plugin.actions;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.util.GrailsUtils;

import java.util.List;

public class NewGrailsFilterAction extends NewGrailsXXXAction {

  public NewGrailsFilterAction() {
    super("action.Grails.NewFilter.text");
  }

  @Override
  protected boolean isEnabled(@NotNull GrailsApplication application) {
    // todo substitute following with application instanceof Grails3Application
    return application.getGrailsVersion().isLessThan("3.0");
  }

  @Override
  protected @NotNull String getCommand(@NotNull GrailsApplication application) {
    return "create-filters";
  }

  @Override
  protected @Nullable VirtualFile getTargetDirectory(@NotNull GrailsApplication application) {
    return GrailsUtils.findConfDirectory(application);
  }

  @Override
  protected void fillGeneratedNamesList(@NotNull String name, @NotNull List<String> names) {
    names.add("grails-app/conf/" + canonicalize(name) + "Filters.groovy");
    names.add(GrailsUtils.GRAILS_UNIT_TESTS + canonicalize(name) + "FiltersSpec.groovy");
  }
}

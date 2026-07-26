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

package org.apache.grails.intellij.plugin.artefact.impl;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.artefact.api.GrailsArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.api.IconOwner;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;

import javax.swing.Icon;

public final class FilterArtefactHandler implements GrailsArtefactHandler, IconOwner {

  public static final FilterArtefactHandler INSTANCE = new FilterArtefactHandler();

  private FilterArtefactHandler() {
  }

  @Override
  public @NotNull String getArtefactHandlerID() {
    return "Filters";
  }

  @Override
  public @Nullable VirtualFile getDirectory(@NotNull GrailsApplication application) {
    return application.getAppRoot().findChild("conf");
  }

  @Override
  public @NotNull Icon getIcon() {
    return AllIcons.General.Filter;
  }
}

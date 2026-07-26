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

package org.jetbrains.plugins.grails.artefact.impl;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler;
import org.jetbrains.plugins.grails.projectView.NodeWeights;
import org.jetbrains.plugins.grails.structure.GrailsApplication;

import javax.swing.Icon;
import java.util.Collection;
import java.util.List;

public final class DomainArtefactHandler implements GrailsDisplayableArtefactHandler {

  public static final DomainArtefactHandler INSTANCE = new DomainArtefactHandler();

  private DomainArtefactHandler() {
  }

  @Override
  public @NotNull String getArtefactHandlerID() {
    return "Domain";
  }

  @Override
  public @NotNull String getArtefactClassSuffix() {
    return "";
  }

  @Override
  public @NotNull Collection<String> getAnnotationFqns() {
    return List.of("grails.persistence.Entity", "grails.gorm.annotation.Entity");
  }

  @Override
  public @Nullable VirtualFile getDirectory(@NotNull GrailsApplication application) {
    return application.getAppRoot().findChild("domain");
  }

  @Override
  public @NotNull Icon getIcon() {
    return AllIcons.Nodes.DataTables;
  }

  @Override
  public @NotNull Icon getGroupIcon() {
    return AllIcons.Nodes.Models;
  }

  @Override
  public @NotNull String getTitle() {
    return "Domain Classes";
  }

  @Override
  public int getWeight() {
    return NodeWeights.DOMAIN_CLASSES_FOLDER;
  }
}

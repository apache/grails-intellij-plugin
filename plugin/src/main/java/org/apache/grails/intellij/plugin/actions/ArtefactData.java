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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;

/**
 * The Grails artefact the current data context points at, resolved from either the artefact class
 * itself or one of its views.
 *
 * @see GrailsActionUtil#getArtefactData
 */
public final class ArtefactData {

  private final @NotNull Project myProject;
  private final @NotNull Module myModule;
  private final @NotNull VirtualFile myFile;
  private final @Nullable String myPackageName;
  private final @NotNull String myArtefactName;
  private final @NotNull GrailsApplication myApplication;
  private final boolean myIsView;

  public ArtefactData(@NotNull Project project,
                      @NotNull Module module,
                      @NotNull VirtualFile file,
                      @Nullable String packageName,
                      @NotNull String artefactName,
                      @NotNull GrailsApplication application,
                      boolean isView) {
    myProject = project;
    myModule = module;
    myFile = file;
    myPackageName = packageName;
    myArtefactName = artefactName;
    myApplication = application;
    myIsView = isView;
  }

  public @NotNull Project getProject() {
    return myProject;
  }

  public @NotNull Module getModule() {
    return myModule;
  }

  public @NotNull VirtualFile getFile() {
    return myFile;
  }

  /** {@code null} when resolved from a view, where the package is not known. */
  public @Nullable String getPackageName() {
    return myPackageName;
  }

  public @NotNull String getArtefactName() {
    return myArtefactName;
  }

  public @NotNull GrailsApplication getApplication() {
    return myApplication;
  }

  public boolean isView() {
    return myIsView;
  }
}

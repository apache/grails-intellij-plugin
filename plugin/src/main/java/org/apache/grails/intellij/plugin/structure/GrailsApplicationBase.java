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
package org.apache.grails.intellij.plugin.structure;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.config.GrailsConstants;

import javax.swing.Icon;
import java.util.Objects;

public abstract class GrailsApplicationBase extends UserDataHolderBase implements GrailsApplication {

  private final Project myProject;
  private final VirtualFile myRoot;
  private boolean myValid = true;

  protected GrailsApplicationBase(@NotNull Project project, @NotNull VirtualFile root) {
    myProject = project;
    myRoot = root;
  }

  @Override
  public @NotNull Project getProject() {
    return myProject;
  }

  @Override
  public @NotNull VirtualFile getRoot() {
    return myRoot;
  }

  @Override
  public @NotNull VirtualFile getAppRoot() {
    return Objects.requireNonNull(myRoot.findChild(GrailsConstants.APP_DIRECTORY));
  }

  @Override
  public @NotNull Icon getIcon() {
    return GroovyMvcIcons.Grails_app;
  }

  @Override
  public boolean isValid() {
    return myValid && myRoot.findChild(GrailsConstants.APP_DIRECTORY) != null && !myProject.isDisposed();
  }

  @Override
  public void invalidate() {
    myValid = false;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || other.getClass() != getClass()) return false;
    GrailsApplicationBase that = (GrailsApplicationBase)other;
    return myProject.equals(that.myProject) && myRoot.equals(that.myRoot);
  }

  @Override
  public int hashCode() {
    return 31 * myProject.hashCode() + myRoot.hashCode();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName() + "{name: " + getName() + ", root: " + myRoot + ", version: " + getGrailsVersion() + "}";
  }
}

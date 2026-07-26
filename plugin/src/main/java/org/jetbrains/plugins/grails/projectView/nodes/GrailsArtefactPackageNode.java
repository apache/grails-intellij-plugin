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

package org.jetbrains.plugins.grails.projectView.nodes;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public abstract class GrailsArtefactPackageNode<T> extends GrailsArtefactHandlerNodeBase<T> {

  protected GrailsArtefactPackageNode(Project project, @NotNull ViewSettings settings, @NotNull T value) {
    super(project, settings, value);
  }

  @Override
  protected final @NotNull GrailsDisplayableArtefactHandler getArtefactHandler() {
    return TreeNodeUtil.findNotNullValueOfType(this, GrailsDisplayableArtefactHandler.class);
  }

  @Override
  public final int getTypeSortWeight(boolean sortByType) {
    return 3;
  }

  public abstract @NotNull String getPackageFqn();

  @Override
  public final boolean canRepresent(@Nullable Object element) {
    return canRepresentInner(element) || super.canRepresent(element);
  }

  private boolean canRepresentInner(@Nullable Object element) {
    return element instanceof VirtualFile file && file.isDirectory() && getNodeDirectories().contains(file);
  }

  protected abstract @NotNull Collection<VirtualFile> getNodeDirectories();

  protected @NotNull Collection<VirtualFile> packageDirectories(@NotNull String fqn) {
    PsiPackage psiPackage = JavaPsiFacade.getInstance(Objects.requireNonNull(getProject())).findPackage(fqn);
    if (psiPackage == null) return List.of();
    List<VirtualFile> result = new ArrayList<>();
    for (PsiDirectory directory : psiPackage.getDirectories(getScope())) {
      result.add(directory.getVirtualFile());
    }
    return result;
  }
}

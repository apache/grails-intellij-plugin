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

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.TreeViewUtil;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiPackage;
import com.intellij.ui.IconManager;
import com.intellij.ui.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Objects;

class GrailsFlatPackageNode extends GrailsArtefactPackageNode<GrailsPackageValue> {

  private final String packageFqn;

  GrailsFlatPackageNode(Project project, @NotNull ViewSettings settings, @NotNull String packageFqn) {
    super(project, settings, new GrailsPackageValue(packageFqn));
    this.packageFqn = packageFqn;
  }

  @Override
  public @NotNull String getPackageFqn() {
    return packageFqn;
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setIcon(IconManager.getInstance().getPlatformIcon(PlatformIcons.Package));
    String presentableText = packageFqn;
    if (getSettings().isAbbreviatePackageNames()) {
      PsiPackage psiPackage = JavaPsiFacade.getInstance(Objects.requireNonNull(getProject())).findPackage(packageFqn);
      if (psiPackage != null) {
        String abbreviated = TreeViewUtil.calcAbbreviatedPackageFQName(psiPackage);
        if (abbreviated != null) presentableText = abbreviated;
      }
    }
    presentation.setPresentableText(presentableText);
  }

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> getChildren() {
    return GrailsNodes.getClassNodes(getArtefacts(), Objects.requireNonNull(getProject()), getSettings(),
                                     getArtefactHandler(), packageFqn);
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    return !file.isDirectory() && getNodeDirectories().contains(file.getParent());
  }

  @Override
  protected @NotNull Collection<VirtualFile> getNodeDirectories() {
    return packageDirectories(packageFqn);
  }
}

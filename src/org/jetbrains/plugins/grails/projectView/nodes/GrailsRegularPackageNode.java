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
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.ValidateableNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.IconManager;
import com.intellij.ui.PlatformIcons;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

public class GrailsRegularPackageNode extends GrailsArtefactPackageNode<CompactedFqn> implements ValidateableNode {

  public GrailsRegularPackageNode(Project project, @NotNull ViewSettings settings, @NotNull CompactedFqn fqns) {
    super(project, settings, fqns);
  }

  @Override
  public @NotNull String getPackageFqn() {
    return getValue().toString();
  }

  /**
   * This method checks if this node contains compacted fqns and that all compacted fqns are still empty.
   *
   * <p>Consider the structure:
   * <pre>
   * - com
   *   - foo.bar // &lt;- this node
   *     - Something
   * </pre>
   * In the above case we need to ensure that {@code com.foo} package is still empty.
   * If there is something to show under {@code com.foo}, then we consider this node invalid,
   * so {@code com} children would be recomputed, resulting in the new structure:
   * <pre>
   * - com
   *     - foo
   *       - SomethingNew
   *       - bar
   *         - Something
   * </pre>
   */
  @Override
  public boolean isValid() {
    CompactedFqn value = getValue();
    if (value.getHasCompactedFqns() && getSettings().isHideEmptyMiddlePackages()) {
      Collection<List<String>> newPackages = GrailsNodes.getPackagesRegular(getArtefacts(), value.getBaseFqn(), true);
      return newPackages.contains(value.getRelativeParts());
    }
    return true;
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setIcon(IconManager.getInstance().getPlatformIcon(PlatformIcons.Package));
    presentation.setPresentableText(GrailsNodes.fqnString(getValue().getRelativeParts()));
  }

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> getChildren() {
    return GrailsNodes.getNodesRegular(getArtefacts(), Objects.requireNonNull(getProject()), getSettings(),
                                       getArtefactHandler(), getValue().getAllParts());
  }

  @Override
  protected @NotNull Collection<VirtualFile> getNodeDirectories() {
    Collection<VirtualFile> result = new HashSet<>();
    for (String fqn : getValue().getExpandedFqns()) {
      result.addAll(packageDirectories(fqn));
    }
    return result;
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    for (VirtualFile directory : packageDirectories(getValue().getExpandedFqns().get(0))) {
      if (VfsUtil.isAncestor(directory, file, true)) return true;
    }
    return false;
  }
}

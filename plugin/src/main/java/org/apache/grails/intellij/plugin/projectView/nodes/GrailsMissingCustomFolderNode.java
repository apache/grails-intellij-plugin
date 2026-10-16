/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.grails.intellij.plugin.projectView.nodes;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.ui.SimpleTextAttributes;
import com.intellij.ui.tree.LeafState;
import org.apache.grails.intellij.plugin.config.FolderPaths;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * A configured custom folder that does not resolve on disk. It is the only node of this feature left
 * with a plain String value, so the platform's visitor cannot reach through it - which is why a
 * folder that is missing never breaks *Reveal* for the rest of the tree. An unresolvable path keeps
 * its node rather than being dropped, so it stays visible and removable from the settings page.
 */
public final class GrailsMissingCustomFolderNode extends AbstractTreeNode<String> implements GrailsCustomFolder {

  private final String configuredPath;

  public GrailsMissingCustomFolderNode(@NotNull Project project,
                                       @NotNull ViewSettings settings,
                                       @NotNull String configuredPath) {
    super(project, configuredPath);
    this.configuredPath = configuredPath;
  }

  @Override
  public @NotNull String getConfiguredPath() {
    return configuredPath;
  }

  @Override
  public int getNodeWeight() {
    return NodeWeights.CUSTOM_FOLDER;
  }

  /**
   * A folder that does not exist has nothing to expand. Reporting it as a leaf keeps the tree from
   * asking it for children it can never have — the platform computes leaf state by calling
   * {@code getChildren()}, and a node that never settles leaves the pane on "Loading...".
   */
  @Override
  public @NotNull LeafState getLeafState() {
    return LeafState.ALWAYS;
  }

  @Override
  public @NotNull List<AbstractTreeNode<?>> getChildren() {
    return List.of();
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setIcon(AllIcons.Nodes.Folder);
    presentation.addText(FolderPaths.folderName(configuredPath), SimpleTextAttributes.REGULAR_ATTRIBUTES);
    presentation.setPresentableText(FolderPaths.folderName(configuredPath));
    presentation.setTooltip(configuredPath);
  }
}

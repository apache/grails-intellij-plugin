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

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileSystemItemFilter;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFileSystemItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class GrailsPsiDirectoryNode extends PsiDirectoryNode {

  private final Icon nodeIcon;
  private final int nodeWeight;
  private final String nodeTitle;

  public GrailsPsiDirectoryNode(@NotNull PsiDirectory directory, @NotNull ViewSettings settings) {
    this(directory, settings, null, 3, null, null);
  }

  public GrailsPsiDirectoryNode(@NotNull PsiDirectory directory, @NotNull ViewSettings settings, int nodeWeight) {
    this(directory, settings, null, nodeWeight, null, null);
  }

  public GrailsPsiDirectoryNode(@NotNull PsiDirectory directory,
                                @NotNull ViewSettings settings,
                                int nodeWeight,
                                @Nullable PsiFileSystemItemFilter filter) {
    this(directory, settings, null, nodeWeight, null, filter);
  }

  public GrailsPsiDirectoryNode(@NotNull PsiDirectory directory,
                                @NotNull ViewSettings settings,
                                @Nullable Icon nodeIcon,
                                int nodeWeight,
                                @Nullable String nodeTitle) {
    this(directory, settings, nodeIcon, nodeWeight, nodeTitle, null);
  }

  public GrailsPsiDirectoryNode(@NotNull PsiDirectory directory,
                                @NotNull ViewSettings settings,
                                @Nullable Icon nodeIcon,
                                int nodeWeight,
                                @Nullable String nodeTitle,
                                @Nullable PsiFileSystemItemFilter filter) {
    super(directory.getProject(), directory, settings, filter);
    this.nodeIcon = nodeIcon;
    this.nodeWeight = nodeWeight;
    this.nodeTitle = nodeTitle;
  }

  public @Nullable Icon getNodeIcon() {
    return nodeIcon;
  }

  public int getNodeWeight() {
    return nodeWeight;
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    super.updateImpl(data);
    if (nodeIcon != null) data.setIcon(nodeIcon);
    if (nodeTitle != null) data.setPresentableText(nodeTitle);
  }
}

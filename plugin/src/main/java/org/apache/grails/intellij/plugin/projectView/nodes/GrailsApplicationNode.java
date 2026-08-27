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
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.projectView.api.GrailsViewNodeProvider;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;

import java.util.ArrayList;
import java.util.Collection;

public class GrailsApplicationNode extends ProjectViewNode<GrailsApplication> {

  public GrailsApplicationNode(@NotNull GrailsApplication application, @NotNull ViewSettings viewSettings) {
    super(application.getProject(), application, viewSettings);
  }

  @Override
  public boolean shouldUpdateData() {
    return getValue().isValid() && super.shouldUpdateData();
  }

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> getChildren() {
    Collection<AbstractTreeNode<?>> result = new ArrayList<>();
    for (GrailsViewNodeProvider provider : GrailsViewNodeProvider.EP_NAME.getExtensionList()) {
      result.addAll(provider.createNodes(getValue(), getSettings()));
    }
    return result;
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    Project project = getProject();
    return project != null && GrailsApplicationManager.getInstance(project).findApplication(file) == getValue();
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    GrailsApplication application = getValue();
    presentation.setIcon(application.getIcon());
    presentation.addText(application.getName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    String appVersion = application.getAppVersion();
    if (appVersion != null) {
      presentation.addText(" " + appVersion, SimpleTextAttributes.REGULAR_ATTRIBUTES); // NON-NLS
    }
    presentation.addText(" " + GrailsBundle.message("project.view.application.node.version.label",
                                                    application.getGrailsVersion()),
                         SimpleTextAttributes.REGULAR_ATTRIBUTES);
    presentation.setTooltip(application.getRoot().getPath());
  }
}

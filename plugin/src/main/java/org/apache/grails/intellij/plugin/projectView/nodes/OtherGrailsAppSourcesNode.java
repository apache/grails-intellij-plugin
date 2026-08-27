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
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.artefact.api.ArtefactHandlers;
import org.apache.grails.intellij.plugin.artefact.api.GrailsDisplayableArtefactHandler;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;
import org.apache.grails.intellij.plugin.projectView.impl.GrailsViewItems;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class OtherGrailsAppSourcesNode extends GrailsPsiDirectoryNode {

  private volatile GrailsApplication grailsApplication;

  public OtherGrailsAppSourcesNode(@NotNull PsiDirectory directory, @NotNull ViewSettings settings) {
    super(directory, settings, PlatformIcons.SOURCE_FOLDERS_ICON, NodeWeights.OTHER_GRAILS_APP_FOLDER, null);
  }

  public @NotNull GrailsApplication getGrailsApplication() {
    GrailsApplication result = grailsApplication;
    if (result == null) {
      result = TreeNodeUtil.findNotNullValueOfType(this, GrailsApplication.class);
      grailsApplication = result;
    }
    return result;
  }

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> getChildrenImpl() {
    GrailsApplication application = getGrailsApplication();
    Project project = application.getProject();
    PsiManager manager = PsiManager.getInstance(project);
    Set<String> specialNames = GrailsViewItems.SPECIAL_GRAILS_APP_FOLDERS.keySet();

    // directories from nodes that are showed separately
    Set<VirtualFile> visibleArtefactDirs = new LinkedHashSet<>();
    for (GrailsDisplayableArtefactHandler handler : ArtefactHandlers.displayableArtefactHandlers()) {
      if (!handler.isVisible(application)) continue;
      VirtualFile directory = handler.getDirectory(application);
      if (directory != null) visibleArtefactDirs.add(directory);
    }
    List<VirtualFile> artefactDirs = new ArrayList<>();
    for (VirtualFile dir : visibleArtefactDirs) {
      if (!specialNames.contains(dir.getName())) artefactDirs.add(dir);
    }

    // other grails-app directories excluding special, special are showed separately
    List<VirtualFile> otherDirs = new ArrayList<>();
    for (VirtualFile child : application.getAppRoot().getChildren()) {
      if (!artefactDirs.contains(child) && !specialNames.contains(child.getName())) otherDirs.add(child);
    }

    Collection<AbstractTreeNode<?>> result = new ArrayList<>();
    // do not show artefact nodes under these directories
    for (VirtualFile dir : artefactDirs) {
      PsiDirectory directory = manager.findDirectory(dir);
      if (directory != null) {
        result.add(new PsiDirectoryNode(project, directory, getSettings(), GrailsViewItems::shouldShowItem));
      }
    }
    for (VirtualFile dir : otherDirs) {
      PsiDirectory directory = manager.findDirectory(dir);
      if (directory != null) {
        result.add(new PsiDirectoryNode(project, directory, getSettings()));
      }
    }
    return result;
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    if (!super.contains(file)) return false;
    PsiFile psiFile = PsiManager.getInstance(Objects.requireNonNull(getProject())).findFile(file);
    return psiFile != null && GrailsViewItems.shouldShowItem(psiFile);
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    super.updateImpl(data);
    data.setLocationString("Other sources");
  }
}

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

package org.apache.grails.intellij.plugin.projectView.impl;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.artefact.api.ArtefactHandlers;
import org.apache.grails.intellij.plugin.artefact.api.GrailsDisplayableArtefactHandler;
import org.apache.grails.intellij.plugin.projectView.api.GrailsViewNodeProvider;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsArtefactHandlerNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsPsiDirectoryNode;
import org.apache.grails.intellij.plugin.projectView.nodes.OtherGrailsAppSourcesNode;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public final class GrailsAppNodeProvider implements GrailsViewNodeProvider {

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> createNodes(@NotNull GrailsApplication application,
                                                              @NotNull ViewSettings settings) {
    Project project = application.getProject();
    Collection<AbstractTreeNode<?>> result = new ArrayList<>();

    for (GrailsDisplayableArtefactHandler handler : ArtefactHandlers.displayableArtefactHandlers()) {
      if (handler.isVisible(application)) {
        result.add(new GrailsArtefactHandlerNode(project, handler, settings));
      }
    }

    for (Map.Entry<String, GrailsViewItems.SpecialFolder> entry : GrailsViewItems.SPECIAL_GRAILS_APP_FOLDERS.entrySet()) {
      PsiDirectory directory = GrailsViewItems.findAppPsiDirectory(application, entry.getKey());
      if (directory != null) {
        GrailsViewItems.SpecialFolder data = entry.getValue();
        result.add(new GrailsPsiDirectoryNode(directory, settings, data.icon(), data.weight(), data.title(),
                                              GrailsViewItems::shouldShowItem));
      }
    }

    PsiDirectory appRoot = PsiManager.getInstance(project).findDirectory(application.getAppRoot());
    if (appRoot != null) {
      result.add(new OtherGrailsAppSourcesNode(appRoot, settings));
    }

    return result;
  }
}

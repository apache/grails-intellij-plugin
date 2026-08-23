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
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;
import org.apache.grails.intellij.plugin.structure.OldGrailsApplication;

import java.util.ArrayList;
import java.util.Collection;

public class OldGrailsPluginsNode extends ProjectViewNode<String> {

  public OldGrailsPluginsNode(@NotNull Project project, @NotNull ViewSettings settings) {
    super(project, "plugins", settings);
  }

  public @NotNull OldGrailsApplication getGrailsApplication() {
    return (OldGrailsApplication)getParentValue();
  }

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> getChildren() {
    OldGrailsApplication application = getGrailsApplication();
    PsiManager manager = PsiManager.getInstance(application.getProject());
    Collection<AbstractTreeNode<?>> result = new ArrayList<>();
    for (VirtualFile root : GrailsFramework.getInstance().getCommonPluginRoots(application.getModule(), false)) {
      PsiDirectory directory = manager.findDirectory(root);
      if (directory != null) {
        result.add(new GrailsPsiDirectoryNode(directory, getSettings(), GroovyMvcIcons.Groovy_mvc_plugin,
                                              NodeWeights.FOLDER, directory.getName()));
      }
    }
    return result;
  }

  @Override
  protected void update(@NotNull PresentationData data) {
    data.setIcon(GroovyMvcIcons.Groovy_mvc_plugin);
    data.setPresentableText("Plugins");
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    for (AbstractTreeNode<?> child : getChildren()) {
      if (child instanceof GrailsPsiDirectoryNode node && node.contains(file)) return true;
    }
    return false;
  }
}

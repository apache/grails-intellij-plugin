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

package org.jetbrains.plugins.grails.projectView;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GroovyMvcIcons;
import org.jetbrains.plugins.grails.plugins.GrailsPluginDescriptor;
import org.jetbrains.plugins.grails.plugins.ImplKt;
import org.jetbrains.plugins.grails.projectView.nodes.GrailsPluginNode;
import org.jetbrains.plugins.grails.structure.GrailsApplication;

import java.util.ArrayList;
import java.util.Collection;

public class GrailsPluginsNode extends ProjectViewNode<String> {

  public GrailsPluginsNode(@NotNull Project project, @NotNull ViewSettings settings) {
    super(project, "plugins", settings);
  }

  public @NotNull GrailsApplication getGrailsApplication() {
    return (GrailsApplication)getParentValue();
  }

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> getChildren() {
    GrailsApplication application = getGrailsApplication();
    Project project = application.getProject();
    Collection<AbstractTreeNode<?>> result = new ArrayList<>();
    for (GrailsPluginDescriptor descriptor : ImplKt.computePlugins(application)) {
      result.add(new GrailsPluginNode(project, descriptor, getSettings()));
    }
    return result;
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    for (AbstractTreeNode<?> child : getChildren()) {
      if (child instanceof ProjectViewNode<?> node && node.contains(file)) return true;
    }
    return false;
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setIcon(GroovyMvcIcons.Groovy_mvc_plugin);
    presentation.setPresentableText("Plugins");
  }
}

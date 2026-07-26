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

package org.apache.grails.intellij.plugin.projectView.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.AbstractPsiBasedNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiClass;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.plugins.GrailsPluginDescriptor;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;

import java.util.Collection;
import java.util.List;

public class GrailsPluginNode extends AbstractPsiBasedNode<GrailsPluginDescriptor> {

  private volatile GrailsApplication grailsApplication;

  public GrailsPluginNode(Project project, @NotNull GrailsPluginDescriptor value, @NotNull ViewSettings settings) {
    super(project, value, settings);
    myName = value.getPluginName();
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
  protected @Nullable PsiClass extractPsiFromValue() {
    GrailsPluginDescriptor value = getValue();
    return value != null ? value.getPluginClass() : null;
  }

  @Override
  protected @NotNull Collection<AbstractTreeNode<?>> getChildrenImpl() {
    return List.of();
  }

  @Override
  public boolean isAlwaysLeaf() {
    return true;
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    GrailsPluginDescriptor value = getValue();
    data.setIcon(GroovyMvcIcons.Groovy_mvc_plugin);
    data.addText(value.getPluginName(), SimpleTextAttributes.REGULAR_BOLD_ATTRIBUTES);
    String version = value.getPluginVersion();
    data.addText(" " + (version != null ? version : getGrailsApplication().getGrailsVersion()),
                 SimpleTextAttributes.REGULAR_ATTRIBUTES);
  }
}

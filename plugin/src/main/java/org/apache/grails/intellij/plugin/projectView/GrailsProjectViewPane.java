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

package org.apache.grails.intellij.plugin.projectView;

import com.intellij.ide.impl.ProjectViewSelectInTarget;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.AbstractProjectViewPaneWithAsyncSupport;
import com.intellij.ide.projectView.impl.ProjectTreeStructure;
import com.intellij.ide.projectView.impl.ProjectViewTree;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.config.GrailsConstants;
import org.apache.grails.intellij.plugin.projectView.impl.GrailsNodeComparator;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsRootNode;

import javax.swing.Icon;
import javax.swing.tree.DefaultTreeModel;
import java.util.Comparator;

public final class GrailsProjectViewPane extends AbstractProjectViewPaneWithAsyncSupport {

  private final Project project;

  public GrailsProjectViewPane(@NotNull Project project) {
    super(project);
    this.project = project;
  }

  public @NotNull Project getProject() {
    return project;
  }

  @Override
  public @NotNull String getId() {
    return GrailsConstants.GRAILS;
  }

  @Override
  public @NotNull Icon getIcon() {
    return GroovyMvcIcons.Grails;
  }

  @Override
  public @NotNull String getTitle() {
    return getId();
  }

  @Override
  public int getWeight() {
    return 13;
  }

  @Override
  public @NotNull ProjectViewSelectInTarget createSelectInTarget() {
    return new GrailsSelectInTarget(project, getId(), getTitle());
  }

  @Override
  protected @NotNull ProjectTreeStructure createStructure() {
    return new ProjectTreeStructure(project, getId()) {
      @Override
      protected @NotNull AbstractTreeNode<?> createRoot(@NotNull Project project, @NotNull ViewSettings settings) {
        return new GrailsRootNode(project, settings);
      }

      @Override
      public boolean isToBuildChildrenInBackground(@NotNull Object element) {
        return true;
      }
    };
  }

  @Override
  protected @NotNull ProjectViewTree createTree(@NotNull DefaultTreeModel treeModel) {
    return new ProjectViewTree(treeModel);
  }

  @Override
  protected @NotNull Comparator<NodeDescriptor<?>> createComparator() {
    return new GrailsNodeComparator(project, getId());
  }

  @Override
  public boolean isInitiallyVisible() {
    return false;
  }

  private static final class GrailsSelectInTarget extends ProjectViewSelectInTarget implements DumbAware {

    private final String minorViewId;
    private final String title;

    private GrailsSelectInTarget(@NotNull Project project, @NotNull String minorViewId, @NotNull String title) {
      super(project);
      this.minorViewId = minorViewId;
      this.title = title;
    }

    @Override
    public String toString() {
      return title;
    }

    @Override
    public String getMinorViewId() {
      return minorViewId;
    }

    @Override
    public float getWeight() {
      return 5.239f;
    }
  }
}

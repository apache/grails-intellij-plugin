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
import com.intellij.ide.projectView.ProjectViewNode;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;

import java.util.ArrayList;
import java.util.List;

public class GrailsRootNode extends ProjectViewNode<Project> {

  public GrailsRootNode(@NotNull Project project, @NotNull ViewSettings viewSettings) {
    super(project, project, viewSettings);
  }

  @Override
  public @NotNull List<GrailsApplicationNode> getChildren() {
    List<GrailsApplicationNode> result = new ArrayList<>();
    for (GrailsApplication application : GrailsApplicationManager.getInstance(getValue()).getApplications()) {
      result.add(new GrailsApplicationNode(application, getSettings()));
    }
    return result;
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    return GrailsApplicationManager.getInstance(getValue()).findApplication(file) != null;
  }
}

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
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.artefact.api.GrailsDisplayableArtefactHandler;

import java.util.Collection;
import java.util.Objects;

public class GrailsArtefactHandlerNode extends GrailsArtefactHandlerNodeBase<GrailsDisplayableArtefactHandler> {

  public GrailsArtefactHandlerNode(Project project,
                                   @NotNull GrailsDisplayableArtefactHandler artefactHandler,
                                   @NotNull ViewSettings viewSettings) {
    super(project, viewSettings, artefactHandler);
  }

  @Override
  protected @NotNull GrailsDisplayableArtefactHandler getArtefactHandler() {
    return getValue();
  }

  @Override
  protected void update(@NotNull PresentationData presentation) {
    presentation.setIcon(getValue().getGroupIcon());
    presentation.setPresentableText(getValue().getTitle());
  }

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> getChildren() {
    return GrailsNodes.getArtefactNodes(Objects.requireNonNull(getProject()), getSettings(), getValue(), getArtefacts());
  }

  @Override
  public boolean contains(@NotNull VirtualFile file) {
    return file.isDirectory() || TreeNodeUtil.mayContain(getGrailsApplication(), file);
  }
}

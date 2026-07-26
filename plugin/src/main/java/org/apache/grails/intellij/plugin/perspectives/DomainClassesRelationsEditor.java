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

package org.apache.grails.intellij.plugin.perspectives;

import com.intellij.ide.structureView.StructureViewBuilder;
import com.intellij.openapi.graph.builder.components.GraphStructureViewBuilderSetup;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.xml.DomElement;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;

import javax.swing.JComponent;

public class DomainClassesRelationsEditor extends PerspectiveFileEditor {
  private PerspectiveFileEditorComponent myComponent;
  private final VirtualFile domainDirectory;

  private final @NotNull NotNullLazyValue<StructureViewBuilder> myStructureViewBuilder =
    NotNullLazyValue.atomicLazy(() -> GraphStructureViewBuilderSetup.setupFor(getDependenciesComponent().getBuilder(), null));

  protected DomainClassesRelationsEditor(Project project, VirtualFile virtualFile) {
    super(project, virtualFile);

    final Module module = ModuleUtilCore.findModuleForFile(virtualFile, project);
    domainDirectory = GrailsArtifact.DOMAIN.findDirectory(module);
  }

  @Override
  protected @Nullable DomElement getSelectedDomElement() {
    return null;
  }

  @Override
  protected void setSelectedDomElement(DomElement domElement) {
  }

  @Override
  protected @NotNull JComponent createCustomComponent() {
    return getDependenciesComponent();
  }

  @Override
  public @Nullable JComponent getPreferredFocusedComponent() {
    return null;
  }

  @Override
  public @NonNls @NotNull String getName() {
    return GrailsBundle.message("domain.classes.dependencies");
  }

  @Override
  public void commit() {
  }

  @Override
  public void reset() {
    myComponent.reset();
  }

  public PerspectiveFileEditorComponent getDependenciesComponent() {
    if (myComponent == null) {
      myComponent = new PerspectiveFileEditorComponent(domainDirectory, getProject());
      Disposer.register(this, myComponent);
    }

    return myComponent;
  }

  @Override
  public StructureViewBuilder getStructureViewBuilder() {
    return myStructureViewBuilder.getValue();
  }
}

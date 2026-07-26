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
package com.intellij.groovy.grails.maven;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenConstants;
import org.jetbrains.plugins.grails.projectView.api.GrailsSingleNodeProvider;
import org.jetbrains.plugins.grails.structure.GrailsApplication;

/** Shows the {@code pom.xml} of a Maven-built Grails application in the Grails project view. */
public final class MavenNodeProvider extends GrailsSingleNodeProvider {

  @Override
  public @Nullable AbstractTreeNode<?> createNode(@NotNull GrailsApplication application, @NotNull ViewSettings settings) {
    if (!(application instanceof GrailsMavenApplication)) return null;
    Project project = application.getProject();
    VirtualFile pom = application.getRoot().findChild(MavenConstants.POM_XML);
    if (pom == null) return null;
    PsiFile psiFile = PsiManager.getInstance(project).findFile(pom);
    if (psiFile == null) return null;
    return new PsiFileNode(project, psiFile, settings);
  }
}

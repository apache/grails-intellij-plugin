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
package org.apache.grails.intellij.plugin.editor.toolbar;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.util.NlsActions.ActionText;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.GsonConstants;
import org.apache.grails.intellij.plugin.actions.ArtefactData;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class GoToViewAction extends GrailsToolbarVfileAction {

  @Override
  public boolean isOpenSingle() {
    return false;
  }

  @Override
  public @ActionText @NotNull String getTitle(@NotNull ArtefactData artefactData) {
    return GrailsBundle.message("action.text.go.to.views", StringUtil.capitalize(artefactData.getArtefactName()));
  }

  @Override
  public @NotNull Collection<VirtualFile> getNavigateTargets(@NotNull ArtefactData artefactData) {
    // Views for an artefact may live in a different module than the artefact itself (multi-project
    // build): e.g. a service in an upstream project whose views are defined by a downstream app.
    // Search every Grails application belonging to the related-module set (the same set artefact
    // instance discovery uses) rather than only this artefact's own application.
    Set<Module> related = GrailsArtifact.getRelatedModules(artefactData.getModule());
    ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(artefactData.getProject());
    Set<VirtualFile> result = new LinkedHashSet<>();
    for (GrailsApplication application : GrailsApplicationManager.getInstance(artefactData.getProject()).getApplications()) {
      if (!related.contains(fileIndex.getModuleForFile(application.getAppRoot()))) continue;
      VirtualFile views = application.getAppRoot().findChild(GrailsUtils.VIEWS_DIRECTORY);
      if (views == null) continue;
      VirtualFile viewDir = views.findChild(artefactData.getArtefactName());
      if (viewDir == null) continue;
      VirtualFile[] children = viewDir.getChildren();
      if (children == null) continue;
      for (VirtualFile child : children) {
        CharSequence name = child.getNameSequence();
        if (StringUtil.endsWith(name, ".gsp")
            || StringUtil.endsWith(name, ".jsp")
            || StringUtil.endsWith(name, GsonConstants.FILE_SUFFIX)) {
          result.add(child);
        }
      }
    }
    return result;
  }

  @Override
  public @NotNull Collection<AnAction> createGenerateActions(@NotNull ArtefactData artefactData) {
    return List.of(new GenerateViewsAction());
  }
}

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

import com.intellij.openapi.util.NlsActions.ActionText;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.actions.ArtefactData;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

import javax.swing.Icon;
import java.util.Collection;

public abstract class GrailsGoToArtefactActionBase extends GrailsToolbarTargetAction<PsiClass> {

  private final GrailsArtifact myArtefactType;

  protected GrailsGoToArtefactActionBase(GrailsArtifact artefactType) {
    myArtefactType = artefactType;
  }

  protected final GrailsArtifact getArtefactType() {
    return myArtefactType;
  }

  @Override
  public @NlsSafe @NotNull String getTitle(@NotNull ArtefactData artefactData) {
    return StringUtil.capitalize(artefactData.getArtefactName()) + myArtefactType.suffix;
  }

  @Override
  public @NotNull Collection<GrClassDefinition> getNavigateTargets(@NotNull ArtefactData artefactData) {
    // Prefer an artefact in the same package as the current one, but fall back to matching by name
    // alone: in multi-project builds a shared artefact (e.g. a domain in an upstream project) often
    // lives in a different package than the controller/service that uses it.
    Collection<GrClassDefinition> samePackage =
      myArtefactType.getInstances(artefactData.getModule(), artefactData.getPackageName(), artefactData.getArtefactName());
    return samePackage.isEmpty()
           ? myArtefactType.getInstances(artefactData.getModule(), artefactData.getArtefactName())
           : samePackage;
  }

  @Override
  public @ActionText @NotNull String getNavigateTitle(PsiClass target) {
    return GrailsBundle.message("action.text.go.to.artefact", target.getName());
  }

  @Override
  public @Nullable Icon getNavigateIcon(PsiClass target) {
    return myArtefactType.getIcon();
  }

  @Override
  public void navigate(@NotNull ArtefactData artefactData, PsiClass target) {
    target.navigate(true);
  }
}

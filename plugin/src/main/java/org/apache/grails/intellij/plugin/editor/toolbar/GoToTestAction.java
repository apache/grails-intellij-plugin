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
import com.intellij.openapi.util.NlsActions.ActionText;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.actions.ArtefactData;
import org.apache.grails.intellij.plugin.editor.GenerateTestsAction;
import org.apache.grails.intellij.plugin.editor.GrailsEditorToolbar;
import org.apache.grails.intellij.plugin.tests.GrailsTestUtils;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GoToTestAction extends GrailsToolbarVfileAction {

  @Override
  public boolean isOpenSingle() {
    return false;
  }

  @Override
  public @ActionText @NotNull String getTitle(@NotNull ArtefactData artefactData) {
    return GrailsBundle.message("action.text.go.to.tests", StringUtil.capitalize(artefactData.getArtefactName()));
  }

  @Override
  public @NotNull Collection<VirtualFile> getNavigateTargets(@NotNull ArtefactData artefactData) {
    List<VirtualFile> result = new ArrayList<>();

    for (GrailsArtifact artefactType : GrailsEditorToolbar.DECORATED_ARTEFACT_TYPES) {
      for (GrClassDefinition artifact : artefactType.getInstances(artefactData.getModule(),
                                                                 artefactData.getPackageName(),
                                                                 artefactData.getArtefactName())) {
        for (PsiClass testClass : GrailsTestUtils.getTestsForArtifact(artifact, true)) {
          ContainerUtil.addIfNotNull(result, testClass.getContainingFile().getVirtualFile());
        }
      }
    }

    return result;
  }

  @Override
  public @NotNull Collection<AnAction> createGenerateActions(@NotNull ArtefactData artefactData) {
    String capitalized = StringUtil.capitalize(artefactData.getArtefactName());
    List<AnAction> actions = List.of(
      generateTests(false, artefactData, GrailsArtifact.DOMAIN,
                    GrailsBundle.message("action.text.generate.tests.unit", capitalized)),
      generateTests(true, artefactData, GrailsArtifact.DOMAIN,
                    GrailsBundle.message("action.text.generate.tests.integration", capitalized)),
      generateTests(false, artefactData, GrailsArtifact.CONTROLLER,
                    GrailsBundle.message("action.text.generate.controller.tests.unit", capitalized)),
      generateTests(true, artefactData, GrailsArtifact.CONTROLLER,
                    GrailsBundle.message("action.text.generate.controller.tests.integration", capitalized))
    );
    for (AnAction action : actions) {
      action.getTemplatePresentation().setIcon(GroovyMvcIcons.Grails_test);
    }
    return actions;
  }

  private static @NotNull GenerateTestsAction generateTests(boolean integration,
                                                            @NotNull ArtefactData artefactData,
                                                            @NotNull GrailsArtifact artifactType,
                                                            @ActionText String text) {
    GenerateTestsAction action = new GenerateTestsAction(integration, artefactData.getArtefactName(), artifactType);
    action.getTemplatePresentation().setText(text);
    return action;
  }
}

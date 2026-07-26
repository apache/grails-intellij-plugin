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
package org.jetbrains.plugins.grails.editor.toolbar;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.util.NlsActions.ActionText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.actions.ArtefactData;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

public abstract class GenerateControllerActionBase extends GenerateActionBase {

  protected GenerateControllerActionBase(@NotNull String command, @ActionText @Nullable String text) {
    super(command, text, AllIcons.Nodes.Controller);
  }

  /** Offer generation only while the controller does not exist yet. */
  @Override
  public boolean isEnabled(@NotNull ArtefactData data) {
    return findController(data) == null;
  }

  @Override
  public void onDone(@NotNull ArtefactData data) {
    GrClassDefinition controller = findController(data);
    if (controller != null) controller.navigate(true);
  }

  private static @Nullable GrClassDefinition findController(@NotNull ArtefactData data) {
    return single(GrailsArtifact.CONTROLLER.getInstances(data.getModule(), data.getPackageName(), data.getArtefactName()));
  }
}

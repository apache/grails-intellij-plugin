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

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.actions.ArtefactData;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;

import java.util.Collection;
import java.util.List;

public class GoToDomainAction extends GrailsGoToArtefactActionBase {

  public GoToDomainAction() {
    super(GrailsArtifact.DOMAIN);
  }

  @Override
  public @NotNull Collection<AnAction> createGenerateActions(@NotNull ArtefactData artefactData) {
    return List.of(ActionManager.getInstance().getAction("Grails.DomainClass"));
  }
}

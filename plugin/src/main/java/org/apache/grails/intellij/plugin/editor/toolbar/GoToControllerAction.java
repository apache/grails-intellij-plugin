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
package org.apache.grails.intellij.plugin.editor.toolbar;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.util.NlsSafe;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.actions.ArtefactData;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;

import java.util.Collection;
import java.util.List;

public class GoToControllerAction extends GrailsGoToArtefactActionBase {

  public GoToControllerAction() {
    super(GrailsArtifact.CONTROLLER);
  }

  @Override
  public @NlsSafe @NotNull String getTitle(@NotNull ArtefactData artefactData) {
    String actionName = getActionName(artefactData);
    return super.getTitle(artefactData) + (actionName == null ? "" : ":" + actionName);
  }

  @Override
  public void navigate(@NotNull ArtefactData artefactData, PsiClass target) {
    // From a view, jump straight to the action that renders it rather than to the controller class.
    PsiMethod action = GrailsUtils
      .getControllerActions(artefactData.getArtefactName(), artefactData.getModule())
      .get(getActionName(artefactData));
    if (action != null) {
      action.navigate(true);
    }
    else {
      super.navigate(artefactData, target);
    }
  }

  @Override
  public @NotNull Collection<AnAction> createGenerateActions(@NotNull ArtefactData artefactData) {
    return List.of(
      ActionManager.getInstance().getAction("Grails.Controller"),
      new GenerateControllerAction(),
      new GenerateAsyncControllerAction()
    );
  }

  /** The action a view belongs to, or {@code null} for a template (leading underscore). */
  private static @Nullable String getActionName(@NotNull ArtefactData artefactData) {
    if (!artefactData.isView()) return null;
    String name = artefactData.getFile().getNameWithoutExtension();
    return name.startsWith("_") ? null : name;
  }
}

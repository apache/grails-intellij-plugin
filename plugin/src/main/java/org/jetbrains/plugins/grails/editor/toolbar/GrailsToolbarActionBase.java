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

import com.intellij.openapi.actionSystem.ActionGroup;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.util.NlsActions.ActionText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.actions.ArtefactData;
import org.jetbrains.plugins.grails.actions.GrailsActionUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public abstract class GrailsToolbarActionBase extends ActionGroup {

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public boolean displayTextInToolbar() {
    return true;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    ArtefactData data = GrailsActionUtil.getArtefactData(e.getDataContext());
    e.getPresentation().setText(data == null ? getTemplateText() : getTitle(data));
    e.getPresentation().setPerformGroup(isOpenSingle() && data != null && createNavigateActions(data).size() == 1);
    e.getPresentation().setPopupGroup(true);
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    assert isOpenSingle();
    ArtefactData data = GrailsActionUtil.getArtefactData(e.getDataContext());
    if (data == null) return;
    Collection<AnAction> actions = createNavigateActions(data);
    if (actions.size() == 1) {
      actions.iterator().next().actionPerformed(e);
    }
  }

  @Override
  public AnAction @NotNull [] getChildren(@Nullable AnActionEvent e) {
    ArtefactData data = GrailsActionUtil.getArtefactData(e == null ? null : e.getDataContext());
    if (data == null) return AnAction.EMPTY_ARRAY;
    List<AnAction> children = new ArrayList<>(createNavigateActions(data));
    children.add(new Separator());
    children.addAll(createGenerateActions(data));
    return children.toArray(AnAction.EMPTY_ARRAY);
  }

  public boolean isOpenSingle() {
    return true;
  }

  public abstract @ActionText @Nullable String getTitle(@NotNull ArtefactData artefactData);

  public abstract @NotNull Collection<AnAction> createNavigateActions(@NotNull ArtefactData artefactData);

  public @NotNull Collection<AnAction> createGenerateActions(@NotNull ArtefactData artefactData) {
    return Collections.emptyList();
  }
}

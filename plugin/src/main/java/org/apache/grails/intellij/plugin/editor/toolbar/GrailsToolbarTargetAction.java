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
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.util.NlsActions.ActionText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.actions.ArtefactData;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class constructs navigate actions from targets.
 * It does not care about type of navigatable because actual navigate logic is delegated to inheritors.
 *
 * @param <T> type of target, e.g. PsiClass or VirtualFile.
 */
public abstract class GrailsToolbarTargetAction<T> extends GrailsToolbarActionBase {

  @Override
  public @NotNull Collection<AnAction> createNavigateActions(@NotNull ArtefactData artefactData) {
    Collection<? extends T> targets = tryGetNavigateTargets(artefactData);
    List<AnAction> actions = new ArrayList<>(targets.size());
    for (T target : targets) {
      actions.add(new AnAction(getNavigateTitle(target), null, getNavigateIcon(target)) {
        @Override
        public void actionPerformed(@NotNull AnActionEvent e) {
          navigate(artefactData, target);
        }
      });
    }
    return actions;
  }

  public abstract @NotNull Collection<? extends T> getNavigateTargets(@NotNull ArtefactData artefactData);

  public abstract @ActionText @Nullable String getNavigateTitle(T target);

  public abstract @Nullable Icon getNavigateIcon(T target);

  public abstract void navigate(@NotNull ArtefactData artefactData, T target);

  private @NotNull Collection<? extends T> tryGetNavigateTargets(@NotNull ArtefactData artefactData) {
    return DumbService.isDumb(artefactData.getProject())
           ? Collections.emptyList()
           : getNavigateTargets(artefactData);
  }
}

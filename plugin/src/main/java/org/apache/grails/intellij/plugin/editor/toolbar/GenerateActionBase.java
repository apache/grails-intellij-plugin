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

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.util.NlsActions.ActionText;
import com.intellij.openapi.vfs.LocalFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.actions.ArtefactData;
import org.apache.grails.intellij.plugin.actions.GrailsActionUtil;
import org.apache.grails.intellij.plugin.runner.GrailsCommandExecutor;
import org.apache.grails.intellij.plugin.runner.GrailsCommandExecutorUtil;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;

import javax.swing.Icon;
import java.util.Collection;
import java.util.List;

/**
 * Runs a Grails {@code generate-*} command against the domain class the current artefact maps to,
 * then refreshes the application root so the newly written files appear.
 */
public abstract class GenerateActionBase extends AnAction {

  private final String myCommand;

  protected GenerateActionBase(@NotNull String command, @ActionText @Nullable String text, @Nullable Icon icon) {
    super(text, null, icon);
    myCommand = command;
  }

  public @NotNull String getCommand() {
    return myCommand;
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    e.getPresentation().setEnabledAndVisible(isEnabled(e.getDataContext()));
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    ArtefactData data = GrailsActionUtil.getArtefactData(e.getDataContext());
    if (data == null) return;
    GrClassDefinition domainClass = findDomainClass(data);
    if (domainClass == null) return;
    String domainClassName = domainClass.getQualifiedName();
    if (domainClassName == null) return;

    GrailsCommandExecutorUtil.execute(
      data.getApplication(),
      new MvcCommand(myCommand, domainClassName),
      () -> LocalFileSystem.getInstance()
        .refreshFiles(List.of(data.getApplication().getRoot()), true, true, () -> onDone(data)));
  }

  public boolean isEnabled(@NotNull DataContext dataContext) {
    ArtefactData data = GrailsActionUtil.getArtefactData(dataContext);
    if (data == null) return false;
    return findDomainClass(data) != null && isEnabled(data);
  }

  public boolean isEnabled(@NotNull ArtefactData data) {
    return GrailsCommandExecutor.getGrailsExecutor(data.getApplication()) != null;
  }

  public void onDone(@NotNull ArtefactData data) {
  }

  static @Nullable GrClassDefinition findDomainClass(@NotNull ArtefactData data) {
    return single(GrailsArtifact.DOMAIN.getInstances(data.getModule(), data.getPackageName(), data.getArtefactName()));
  }

  /** Kotlin's {@code singleOrNull()}: the element only when there is exactly one. */
  static @Nullable GrClassDefinition single(@NotNull Collection<GrClassDefinition> classes) {
    return classes.size() == 1 ? classes.iterator().next() : null;
  }
}

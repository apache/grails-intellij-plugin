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

package org.apache.grails.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.SizedIcon;
import com.intellij.ui.scale.JBUIScale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.pluginSupport.webflow.WebFlowUtils;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.actions.GroovyTemplatesFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;

import java.io.IOException;

import static org.apache.grails.intellij.plugin.actions.NewGspAction.TEMPLATE_NAME;

public class GrailsGenerateViewAction extends AnAction {

  private static final Logger LOG = Logger.getInstance(GrailsGenerateViewAction.class);

  @Override
  public void actionPerformed(final @NotNull AnActionEvent e) {
    WriteAction.run(() -> createAction(getActionControllerToGenerate(e.getDataContext())));
  }

  public static void createAction(@Nullable Pair<PsiClass, String> pair) {
    if (pair == null) return;

    VirtualFile grailsApp = GrailsArtifact.CONTROLLER.getGrailsApp(pair.first);

    if (grailsApp == null) return;

    try {
      VirtualFile viewDirectory = grailsApp.findChild(GrailsUtils.VIEWS_DIRECTORY);
      if (viewDirectory == null) {
        viewDirectory = grailsApp.createChildDirectory(null, GrailsUtils.VIEWS_DIRECTORY);
      }

      String controllerName = GrailsArtifact.CONTROLLER.getArtifactName(pair.first);

      VirtualFile controllerGspFolder = viewDirectory.findChild(controllerName);
      if (controllerGspFolder == null) {
        controllerGspFolder = viewDirectory.createChildDirectory(null, controllerName);
      }

      PsiDirectory psiDirectory = pair.first.getManager().findDirectory(controllerGspFolder);
      if (psiDirectory == null) return;

      PsiFile psiFile = GroovyTemplatesFactory.createFromTemplate(
        psiDirectory, pair.second + ".gsp", pair.second + ".gsp", TEMPLATE_NAME, true
      );
      if (psiFile == null) return;

      psiFile.navigate(true);
    }
    catch (IOException e1) {
      LOG.warn(e1);
    }
  }

  private static @Nullable Pair<PsiClass, String> getActionControllerToGenerate(@NotNull DataContext dataContext) {
    PsiFile psiFile = CommonDataKeys.PSI_FILE.getData(dataContext);
    if (psiFile == null) return null;

    Editor editor = CommonDataKeys.EDITOR.getData(dataContext);
    if (editor == null) return null;

    PsiElement element = psiFile.findElementAt(editor.getCaretModel().getOffset());

    PsiMember member = PsiTreeUtil.getParentOfType(element, GrField.class, PsiMethod.class);
    if (member == null) return null;

    if (!GrailsArtifact.CONTROLLER.isInstance(member.getContainingClass())) return null;

    return getActionControllerToGenerate(member);
  }

  public static @Nullable Pair<PsiClass, String> getActionControllerToGenerate(@NotNull PsiMember member) {
    String actionName = GrailsUtils.getActionName(member);
    if (actionName == null) return null;

    if (member instanceof GrField && WebFlowUtils.isFlowActionField((GrField)member)) {
      return null;
    }

    return Pair.create(member.getContainingClass(), actionName);
  }

  public static boolean isEnabled(@Nullable Pair<PsiClass, String> pair) {
    return pair != null && GrailsUtils.getViewsByAction(pair.first, pair.second).isEmpty();
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    boolean enabled = isEnabled(getActionControllerToGenerate(e.getDataContext()));

    Presentation presentation = e.getPresentation();
    presentation.setEnabledAndVisible(enabled);

    if (enabled) {
      presentation.setIcon(JBUIScale.scaleIcon(new SizedIcon(GroovyMvcIcons.Gsp_logo, 18, 18)));
    }
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}

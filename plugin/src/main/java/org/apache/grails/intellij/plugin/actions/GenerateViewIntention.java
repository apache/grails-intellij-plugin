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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.Trinity;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.pluginSupport.webflow.WebFlowUtils;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.actions.GroovyTemplatesFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.io.IOException;

import static org.apache.grails.intellij.plugin.actions.NewGspAction.TEMPLATE_NAME;

public final class GenerateViewIntention implements IntentionAction {

  private static final Logger LOG = Logger.getInstance(GenerateViewIntention.class);

  @Override
  public @NotNull String getText() {
    return GrailsBundle.message("intention.text.create.view.gsp.page");
  }

  @Override
  public @NotNull String getFamilyName() {
    return getText();
  }

  private static @Nullable Pair<PsiClass, String> getActionController(@Nullable PsiElement element) {
    if (!(element instanceof PsiMethod) && !(element instanceof GrField)) return null;

    PsiMember member = (PsiMember)element;

    if (!GrailsArtifact.CONTROLLER.isInstance(member.getContainingClass())) return null;

    return GrailsGenerateViewAction.getActionControllerToGenerate(member);
  }

  private static @Nullable PsiElement getElementAtCaret(Editor editor, PsiFile file) {
    PsiElement element = file.findElementAt(editor.getCaretModel().getOffset());
    if (!(element instanceof LeafPsiElement)) return null;

    return element.getParent();
  }

  private static @Nullable Trinity<PsiClass, String, String> getWebFlowState(@Nullable PsiElement element) {
    if (!(element instanceof GrReferenceExpression)) return null;

    PsiElement parent = element.getParent();
    if (parent instanceof GrMethodCall stateDeclarration) {
      if (WebFlowUtils.isStateDeclaration(stateDeclarration, true)) {
        GrField actionField = WebFlowUtils.getActionByStateDeclaration(stateDeclarration);
        PsiClass controller = actionField.getContainingClass();
        String stateName = WebFlowUtils.getStateNameByStateDeclaration(stateDeclarration);
        
        String actionName = StringUtil.trimEnd(actionField.getName(), WebFlowUtils.FLOW_SUFFIX);
        
        return Trinity.create(controller,  actionName, stateName);
      }
    }

    return null;
  }

  private static boolean isEnabledWebFlowCreateViewAction(@Nullable Trinity<PsiClass, String, String> trinity) {
    if (trinity == null) return false;
    
    VirtualFile controllerViewFolder = GrailsUtils.getControllerGspDir(trinity.first);
    if (controllerViewFolder == null) return true;

    VirtualFile flowDir = controllerViewFolder.findChild(trinity.second);
    if (flowDir == null) return true;
    
    if (!flowDir.isDirectory()) return false;

    return flowDir.findChild(trinity.third + ".gsp") == null && flowDir.findChild(trinity.third + ".jsp") == null;
  }

  private static void createWebFlowView(PsiClass controller, String actionName, String stateName) {
    VirtualFile grailsApp = GrailsArtifact.CONTROLLER.getGrailsApp(controller);

    if (grailsApp == null) return;

    try {
      VirtualFile viewDirectory = grailsApp.findChild(GrailsUtils.VIEWS_DIRECTORY);
      if (viewDirectory == null) {
        viewDirectory = grailsApp.createChildDirectory(null, GrailsUtils.VIEWS_DIRECTORY);
      }

      String controllerName = GrailsArtifact.CONTROLLER.getArtifactName(controller);

      VirtualFile controllerGspFolder = viewDirectory.findChild(controllerName);
      if (controllerGspFolder == null) {
        controllerGspFolder = viewDirectory.createChildDirectory(null, controllerName);
      }

      VirtualFile actionFlowFolder = controllerGspFolder.findChild(actionName);
      if (actionFlowFolder == null) {
        actionFlowFolder = controllerGspFolder.createChildDirectory(null, actionName);
      }

      PsiDirectory psiDirectory = controller.getManager().findDirectory(actionFlowFolder);
      if (psiDirectory == null) return;

      String viewName = stateName + ".gsp";
      PsiFile psiFile = GroovyTemplatesFactory.createFromTemplate(psiDirectory, viewName, viewName, TEMPLATE_NAME, true);
      if (psiFile == null) return;

      psiFile.navigate(true);
    }
    catch (IOException e1) {
      LOG.warn(e1);
    }
  }

  @Override
  public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile psiFile) {
    PsiElement element = getElementAtCaret(editor, psiFile);
    return GrailsGenerateViewAction.isEnabled(getActionController(element)) || isEnabledWebFlowCreateViewAction(getWebFlowState(element));
  }

  @Override
  public void invoke(@NotNull Project project, Editor editor, PsiFile psiFile) throws IncorrectOperationException {
    PsiElement element = getElementAtCaret(editor, psiFile);

    Trinity<PsiClass, String, String> trinity = getWebFlowState(element);
    if (trinity != null) {
      createWebFlowView(trinity.first, trinity.second, trinity.third);
      return;
    }

    Pair<PsiClass, String> pair = getActionController(element);

    GrailsGenerateViewAction.createAction(pair);
  }

  @Override
  public boolean startInWriteAction() {
    return true;
  }
}

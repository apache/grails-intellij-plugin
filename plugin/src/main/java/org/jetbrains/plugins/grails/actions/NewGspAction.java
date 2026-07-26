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

package org.jetbrains.plugins.grails.actions;

import com.intellij.ide.IdeView;
import com.intellij.ide.actions.WeighingNewActionGroup;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.util.NlsContexts.DialogMessage;
import com.intellij.openapi.util.NlsContexts.DialogTitle;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.actions.GroovyTemplatesFactory;
import org.jetbrains.plugins.groovy.actions.NewGroovyActionBase;

public class NewGspAction extends NewGroovyActionBase {
  private static final String POINT_GSP = ".gsp";
  public static final String TEMPLATE_NAME = "Groovy Server Page" + POINT_GSP;

  @Override
  protected @NotNull String getActionName(@NotNull PsiDirectory directory, @NotNull String newName) {
    return GrailsBundle.message("gsp.dlg.title");
  }

  @Override
  protected @DialogMessage String getDialogPrompt() {
    return GrailsBundle.message("gsp.dlg.prompt");
  }

  @Override
  protected @DialogTitle String getDialogTitle() {
    return GrailsBundle.message("gsp.dlg.title");
  }

  @Override
  protected boolean isAvailable(DataContext dataContext) {
    return super.isAvailable(dataContext) && isInWebAppOrGrailsViewsDirectory(dataContext);
  }

  @Override
  public void update(final @NotNull AnActionEvent e) {
    final Presentation presentation = e.getPresentation();

    super.update(e);

    if (presentation.isEnabled()) {
      final IdeView view = e.getData(LangDataKeys.IDE_VIEW);
      if (view != null) {
        for (PsiDirectory dir : view.getDirectories()) {
          if (GrailsUtils.isUnderGrailsViewsDirectory(dir)) {
            presentation.putClientProperty(WeighingNewActionGroup.WEIGHT_KEY, WeighingNewActionGroup.HIGHER_WEIGHT);
            return;
          }
        }
      }
    }
  }

  private static boolean isInWebAppOrGrailsViewsDirectory(final DataContext dataContext) {
    final IdeView view = LangDataKeys.IDE_VIEW.getData(dataContext);
    if (view == null) {
      return false;
    }

    final GrailsApplication application = GrailsActionUtil.getGrailsApplication(dataContext);
    if (application == null) {
      return false;
    }

    for (PsiDirectory dir : view.getDirectories()) {
      if (GrailsUtils.isUnderWebAppDirectory(application, dir) || GrailsUtils.isUnderGrailsViewsDirectory(dir)) {
        return true;
      }
    }

    return false;
  }

  @Override
  protected PsiElement @NotNull [] doCreate(String newName, PsiDirectory directory) throws Exception {
    newName = StringUtil.trimEnd(newName, POINT_GSP);

    PsiFile psiFile;

    if (newName.startsWith("_")) {
      // Create an empty file. Don't use file templates if created GSP file is a GSP-template.
      psiFile = directory.createFile(newName + POINT_GSP);
    }
    else {
      psiFile = GroovyTemplatesFactory.createFromTemplate(directory, newName, newName + POINT_GSP, TEMPLATE_NAME, true);
    }

    return new PsiElement[]{psiFile};
  }

}

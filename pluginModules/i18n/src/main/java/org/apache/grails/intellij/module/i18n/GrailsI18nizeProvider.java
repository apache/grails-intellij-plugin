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

package org.apache.grails.intellij.module.i18n;

import com.intellij.codeInspection.i18n.I18nQuickFixHandler;
import com.intellij.codeInspection.i18n.I18nizeHandlerProvider;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspFile;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFileBase;

final class GrailsI18nizeProvider extends I18nizeHandlerProvider {

  @Override
  public I18nQuickFixHandler<?> getHandler(@NotNull PsiFile psiFile, @NotNull Editor editor, @NotNull TextRange range) {
    if (psiFile instanceof GspFile) {
      PsiElement elementAt = psiFile.getViewProvider().findElementAt(range.getStartOffset());
      if (elementAt != null && elementAt.getLanguage() == GroovyLanguage.INSTANCE) {
        if (GrailsI18nGroovyQuickFixHandler.calculatePropertyValue(editor, psiFile) != null) {
          return GrailsI18nGroovyQuickFixHandler.INSTANCE;
        }

        return null;
      }

      return GrailsI18nQuickFixHandler.INSTANCE;
    }

    if (psiFile instanceof GroovyFile && isApplicableGroovyFile((GroovyFile)psiFile)) {
      return GrailsI18nGroovyQuickFixHandler.INSTANCE;
    }

    return null;
  }

  public static boolean isApplicableGroovyFile(@NotNull GroovyFileBase groovyFile) {
    PsiClass[] classes = groovyFile.getClasses();
    if (classes.length == 1) {
      PsiClass aClass = classes[0];
      return GrailsArtifact.CONTROLLER.isInstance(aClass) || GrailsArtifact.TAGLIB.isInstance(aClass);
    }

    return false;
  }
}

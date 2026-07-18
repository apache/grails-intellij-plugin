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

package org.jetbrains.plugins.grails.lang.gsp.debug;

import com.intellij.debugger.engine.evaluation.TextWithImports;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.JavaCodeFragment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.GspFileViewProvider;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.debugger.GroovyCodeFragmentFactory;

public final class GspCodeFragmentFactory extends GroovyCodeFragmentFactory {

  private static PsiElement convertContext(PsiElement context) {
    if (context == null) return null;

    if (context.getLanguage().equals(GroovyLanguage.INSTANCE)) return context;

    PsiFile file = context.getContainingFile();
    if (file == null) return context;

    FileViewProvider viewProvider = file.getViewProvider();
    if (!(viewProvider instanceof GspFileViewProvider)) return context;

    PsiElement e = viewProvider.findElementAt(context.getTextOffset(), GroovyLanguage.INSTANCE);

    while (e != null && !e.getLanguage().equals(GroovyLanguage.INSTANCE)) {
      e = e.getNextSibling();
    }

    return e == null ? context : e;
  }

  @Override
  public JavaCodeFragment createPsiCodeFragmentImpl(TextWithImports textWithImports, PsiElement context, @NotNull Project project) {
    return super.createPsiCodeFragmentImpl(textWithImports, convertContext(context), project);
  }

  @Override
  public JavaCodeFragment createPresentationPsiCodeFragmentImpl(@NotNull TextWithImports item, PsiElement context, @NotNull Project project) {
    return super.createPresentationPsiCodeFragmentImpl(item, convertContext(context), project);
  }

  @Override
  public boolean isContextAccepted(PsiElement context) {
    if (context == null) return false;
    PsiFile file = context.getContainingFile();
    if (file == null || !(file.getViewProvider() instanceof GspFileViewProvider)) return false;

    return super.isContextAccepted(convertContext(context)) && !super.isContextAccepted(context);
  }
}

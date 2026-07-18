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

package org.jetbrains.plugins.grails.lang.gsp.psi;

import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.GspFile;

public final class GspPsiUtil {

  private GspPsiUtil() {
  }

  public static @Nullable GspFile getGspFile(final PsiElement element) {
    if (element == null) return null;
    final PsiFile containingFile = element.getContainingFile();
    if (containingFile == null) return null;

    final FileViewProvider viewProvider = containingFile.getViewProvider();
    final PsiFile psiFile = viewProvider.getPsi(viewProvider.getBaseLanguage());
    return psiFile instanceof GspFile ? (GspFile) psiFile : null;
  }

  public static boolean isInGspFile(PsiElement element) {
    return getGspFile(element) != null;
  }

}

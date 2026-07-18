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

package org.jetbrains.plugins.grails.lang.gsp;

import com.intellij.lang.documentation.AbstractDocumentationProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.javadoc.PsiDocTag;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.groovydoc.psi.api.GrDocTagValueToken;

public final class GspTagDocumentationProvider extends AbstractDocumentationProvider {

  @Override
  public @Nls String generateDoc(PsiElement element, @Nullable PsiElement originalElement) {
    if (element instanceof GrDocTagValueToken) {
      PsiClass aClass = PsiTreeUtil.getParentOfType(element, PsiClass.class);

      if (GrailsArtifact.TAGLIB.isInstance(aClass)) {
        PsiElement parent = element.getParent();

        if (parent instanceof PsiDocTag) {
          PsiElement[] dataElements = ((PsiDocTag)parent).getDataElements();

          if (dataElements.length > 1) {
            @Nls StringBuilder sb = new StringBuilder();
            for (int i = 1; i < dataElements.length; i++) {
              sb.append(dataElements[i].getText());
            }

            return sb.toString();
          }
        }
      }
    }

    return null;
  }
}

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

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.GspFileViewProvider;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.references.common.TemplateFileReferenceSet;
import org.jetbrains.plugins.grails.util.GrailsUtils;

public class GspTemplateAttributeSupport extends TagAttributeReferenceProvider {

  protected GspTemplateAttributeSupport() {
    super("template", "g", null);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         final @NotNull String text,
                                                         final int offset,
                                                         final @NotNull GspTagWrapper gspTagWrapper) {

    final String controllerName;

    PsiFile psiFile = element.getContainingFile();
    VirtualFile file;
    if (psiFile != null && psiFile.getViewProvider() instanceof GspFileViewProvider && (file = psiFile.getOriginalFile().getVirtualFile()) != null) {
      controllerName = GrailsUtils.getExistingControllerNameDirByGsp(file, psiFile.getProject());
    }
    else {
      controllerName = null;
    }

    if (!text.startsWith("/") && controllerName == null) {
      return PsiReference.EMPTY_ARRAY;
    }

    PsiElement pluginAttribute = gspTagWrapper.getAttributeValue("plugin");

    if (pluginAttribute != null) {
      if (gspTagWrapper.getAttributeText(pluginAttribute) == null) {
        return PsiReference.EMPTY_ARRAY;
      }
    }
    else {
      PsiElement contextPathAttribute = gspTagWrapper.getAttributeValue("contextPath");
      if (contextPathAttribute != null) {
        if (gspTagWrapper.getAttributeText(contextPathAttribute) == null) {
          return PsiReference.EMPTY_ARRAY;
        }
      }
    }

    TemplateFileReferenceSet set = new TemplateFileReferenceSet(controllerName, PathReference.trimPath(text), element, offset, null, true, true, gspTagWrapper);

    return set.getAllReferences();
  }

}

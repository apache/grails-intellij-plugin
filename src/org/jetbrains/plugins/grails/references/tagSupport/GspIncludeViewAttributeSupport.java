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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GrailsFileReferenceSetBase;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.grails.util.GrailsUtils;

public class GspIncludeViewAttributeSupport extends TagAttributeReferenceProvider {
  protected GspIncludeViewAttributeSupport() {
    super("view", "g", new String[]{"include"});
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    final VirtualFile viewsDirectory = GrailsUtils.findViewsDirectory(element);
    if (viewsDirectory == null) return PsiReference.EMPTY_ARRAY;

    GrailsFileReferenceSetBase set = new GrailsFileReferenceSetBase(text, element, offset, null, true, true) {
      @Override
      protected VirtualFile getDefaultContext(boolean isAbsolute) {
        return viewsDirectory;
      }
    };

    return set.getAllReferences();
  }
}

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

package org.apache.grails.intellij.plugin.references.tagSupport;

import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.apache.grails.intellij.plugin.references.common.GrailsFileReferenceSetBase;
import org.apache.grails.intellij.plugin.references.common.GspTagWrapper;

import java.util.List;

public class GspSrcJsAttributeSupport extends TagAttributeReferenceProvider {
  protected GspSrcJsAttributeSupport() {
    super("src", "g", new String[]{"javascript"});
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    List<String> attributeNames = gspTagWrapper.getAttributeNames();
    
    if (attributeNames.size() != 1) return PsiReference.EMPTY_ARRAY;

    VirtualFile root = GrailsFramework.getInstance().findAppRoot(element);
    final VirtualFile jsFolder = VfsUtil.findRelativeFile(root, "web-app", "js");
    if (jsFolder == null) return PsiReference.EMPTY_ARRAY;

    GrailsFileReferenceSetBase set = new GrailsFileReferenceSetBase(text, element, offset, null, true, true) {
      @Override
      protected VirtualFile getDefaultContext(boolean isAbsolute) {
        return jsFolder;
      }
    };

    return set.getAllReferences();
  }
}

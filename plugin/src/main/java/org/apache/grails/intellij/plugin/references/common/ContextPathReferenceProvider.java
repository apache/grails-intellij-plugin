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

package org.apache.grails.intellij.plugin.references.common;

import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.apache.grails.intellij.plugin.util.GrailsUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ContextPathReferenceProvider extends PsiReferenceProvider {

  public static PsiReference[] createReferences(@NotNull PsiElement element) {
    TextRange range = ElementManipulators.getValueTextRange(element);
    int offset = range.getStartOffset();
    String text = range.substring(element.getText());
    
    return createReferences(element, text, offset);
  }
  
  public static PsiReference[] createReferences(@NotNull PsiElement element, @NotNull String text, int offset) {
    String trimedUrl = PathReference.trimPath(text);

    if (trimedUrl.trim().isEmpty()) return PsiReference.EMPTY_ARRAY;

    final FileReferenceSet set = new PluginSupportFileReferenceSet(trimedUrl, element, offset, null, true, false, true) {
      @Override
      public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
        if (!isAbsolutePathReference()) {
          return Collections.emptySet();
        }

        VirtualFile appDir = GrailsFramework.getInstance().findAppDirectory(getElement());
        if (appDir == null) return Collections.emptySet();

        PsiManager manager = getElement().getManager();
        List<PsiFileSystemItem> res = new ArrayList<>(2);

        VirtualFile view = appDir.findChild(GrailsUtils.VIEWS_DIRECTORY);
        if (view != null) {
          ContainerUtil.addIfNotNull(res, manager.findDirectory(view));
        }

        VirtualFile root = appDir.getParent();
        if (root != null) {
          ContainerUtil.addIfNotNull(res, manager.findDirectory(root));
        }

        return res;
      }
    };

    return set.getAllReferences();
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    return createReferences(element);
  }
}

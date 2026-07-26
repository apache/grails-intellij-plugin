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

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.common.GspTagWrapper;
import org.apache.grails.intellij.plugin.references.urlMappings.UrlMappingUtil;

import java.util.Map;

public class GspMappingAttributeSupport extends TagAttributeReferenceProvider {

  protected GspMappingAttributeSupport() {
    super("mapping", "g", null);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) return PsiReference.EMPTY_ARRAY;

    PsiReference ref = new PsiPolyVariantReferenceBase<>(element, false) {

      @Override
      public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
        Map<String, UrlMappingUtil.NamedUrlMapping> map = UrlMappingUtil.getNamedUrlMappings(module);
        UrlMappingUtil.NamedUrlMapping mapping = map.get(getValue());

        if (mapping != null) {
          return new ResolveResult[]{new PsiElementResolveResult(mapping.getElement())};
        }

        return ResolveResult.EMPTY_ARRAY;
      }

      @Override
      public Object @NotNull [] getVariants() {
        Map<String, UrlMappingUtil.NamedUrlMapping> map = UrlMappingUtil.getNamedUrlMappings(module);
        return map.keySet().toArray();
      }
    };

    return new PsiReference[]{ref};
  }

}

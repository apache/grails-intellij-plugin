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
package org.jetbrains.plugins.grails.gson;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrScriptField;

public final class GsonControllerReference extends PsiPolyVariantReferenceBase<GrArgumentLabel> {

  public GsonControllerReference(@NotNull GrArgumentLabel element) {
    super(element, true);
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    PsiElement resolved = resolve();
    return resolved == null ? ResolveResult.EMPTY_ARRAY : new ResolveResult[]{new PsiElementResolveResult(resolved)};
  }

  @Override
  public @Nullable PsiElement resolve() {
    GrArgumentLabel element = getElement();
    for (GrScriptField field : GsonUtils.getModelFields(element)) {
      if (field.getName().equals(element.getName())) return field;
    }
    return null;
  }
}

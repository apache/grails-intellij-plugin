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
package org.apache.grails.intellij.plugin.gson;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.findUsages.GroovyReadWriteAccessDetector;

public final class GsonRWAccessDetector extends GroovyReadWriteAccessDetector {

  @Override
  public boolean isReadWriteAccessible(@NotNull PsiElement element) {
    return GsonUtils.isModelVariable(element);
  }

  @Override
  public boolean isDeclarationWriteAccess(@NotNull PsiElement element) {
    // A model-closure variable is populated by the controller, not by its own declaration.
    return !GsonUtils.isModelVariable(element) && super.isDeclarationWriteAccess(element);
  }

  @Override
  public @NotNull Access getReferenceAccess(@NotNull PsiElement referencedElement, @NotNull PsiReference reference) {
    return reference instanceof GsonControllerReference
           ? Access.Write
           : super.getReferenceAccess(referencedElement, reference);
  }

  @Override
  public @NotNull Access getExpressionAccess(@NotNull PsiElement expression) {
    return GsonPatterns.CONTROLLER_REFERENCE_PLACE.accepts(expression)
           ? Access.Write
           : super.getExpressionAccess(expression);
  }
}

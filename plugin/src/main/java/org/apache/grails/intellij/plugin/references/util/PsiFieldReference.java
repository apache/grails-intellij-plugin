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

package org.apache.grails.intellij.plugin.references.util;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiTypes;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrAccessorMethod;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyPropertyUtils;

public abstract class PsiFieldReference extends PsiReferenceBase<PsiElement> {

  public PsiFieldReference(PsiElement element, boolean soft) {
    super(element, soft);
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {

    PsiElement resolve = resolve();
    if (resolve instanceof PsiMethod && !(resolve instanceof GrAccessorMethod)) {
      String s = GroovyPropertyUtils.getPropertyNameByGetterName(newElementName, PsiTypes.booleanType().equals(
        ((PsiMethod)resolve).getReturnType()));
      if (s == null) return getElement();
      newElementName = s;
    }

    PsiElement res = super.handleElementRename(newElementName);
    setRangeInElement(null);
    return res;
  }

}

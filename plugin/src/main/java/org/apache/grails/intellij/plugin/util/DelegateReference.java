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

package org.apache.grails.intellij.plugin.util;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class DelegateReference extends PsiReferenceBase<PsiElement> {

  private volatile boolean myDelegateInit;

  private volatile PsiReference myDelegate;

  private Boolean mySoft;

  public DelegateReference(PsiElement element, TextRange range) {
    super(element, range);
  }

  public DelegateReference(@NotNull PsiElement element) {
    super(element);
  }

  public DelegateReference(@NotNull PsiElement element, boolean isSoft) {
    super(element);
    mySoft = isSoft;
  }

  protected abstract @Nullable PsiReference createDelegate();

  private void ensureInit() {
    if (myDelegateInit) return;

    myDelegate = createDelegate();
    myDelegateInit = true;
  }

  @Override
  public PsiElement resolve() {
    ensureInit();
    return myDelegate == null ? null : myDelegate.resolve();
  }

  @Override
  public Object @NotNull [] getVariants() {
    ensureInit();
    return myDelegate == null ? ArrayUtilRt.EMPTY_OBJECT_ARRAY : myDelegate.getVariants();
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    ensureInit();
    return myDelegate == null ? myElement : myDelegate.handleElementRename(newElementName);
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    ensureInit();
    return myDelegate == null ? myElement : myDelegate.bindToElement(element);
  }

  @Override
  public boolean isReferenceTo(@NotNull PsiElement element) {
    ensureInit();
    return myDelegate != null && myDelegate.isReferenceTo(element);
  }

  @Override
  public boolean isSoft() {
    if (mySoft != null) {
      return mySoft;
    }
    ensureInit();
    return myDelegate == null || myDelegate.isSoft();
  }
}

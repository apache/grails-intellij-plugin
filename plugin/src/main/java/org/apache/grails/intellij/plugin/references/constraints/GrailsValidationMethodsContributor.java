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

package org.apache.grails.intellij.plugin.references.constraints;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

final class GrailsValidationMethodsContributor extends NonCodeMembersContributor {
  // #CHECK# See WebMetaUtils.enhanceCommandObject()
  private static final String CLASS_SOURCES = "class CommandObjectClass {" +
                                              " private void setErrors(org.springframework.validation.Errors errors) {}" +
                                              " private org.springframework.validation.Errors getErrors() {}" +
                                              " private boolean hasErrors() {}" +
                                              " private boolean validate() {}" +
                                              "}";

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return;

    if (aClass == null || !GrailsUtils.isValidatedClass(aClass)) return;

    String nameHint = ResolveUtil.getNameHint(processor);

    for (PsiMethod method : DynamicMemberUtils.getMembers(aClass.getProject(), CLASS_SOURCES).getDynamicMethods(nameHint)) {
      if (!processor.execute(method, state)) return;
    }
  }
}

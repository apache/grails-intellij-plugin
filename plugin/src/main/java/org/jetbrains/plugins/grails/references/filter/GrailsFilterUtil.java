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

package org.jetbrains.plugins.grails.references.filter;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

public final class GrailsFilterUtil {

  private GrailsFilterUtil() {
  }

  public static boolean isFilterDefinitionMethod(PsiElement methodCall) {
    if (!(methodCall instanceof GrMethodCall)) return false;

    PsiClass aClass = PsiTreeUtil.getParentOfType(methodCall, PsiClass.class);
    if (!GrailsArtifact.FILTER.isInstance(aClass)) return false;

    PsiMethod method = ((GrMethodCall)methodCall).resolveMethod();

    return FilterClosureMemberProvider.isFilterDefinitionMethod(method);
  }

  //public static boolean isFilterClosure(GrClosableBlock closure) {
  //  PsiElement eField = closure.getParent();
  //
  //  if (!(eField instanceof GrField)) return false;
  //  GrField field = (GrField)eField;
  //
  //  if (!"filters".equals(field.getName()) || field.getDeclaredType() != null) return false;
  //
  //  PsiClass aClass = field.getContainingClass();
  //
  //  return GrailsArtifact.FILTER.isInstance(aClass);
  //}
  //
}

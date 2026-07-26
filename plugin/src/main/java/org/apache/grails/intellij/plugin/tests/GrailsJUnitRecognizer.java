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

package org.apache.grails.intellij.plugin.tests;

import com.intellij.codeInsight.AnnotationUtil;
import com.intellij.execution.JUnitRecognizer;
import com.intellij.execution.junit.JUnitUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.ext.spock.SpockUtils;

public final class GrailsJUnitRecognizer extends JUnitRecognizer {
  @Override
  public boolean isTestAnnotated(@NotNull PsiMethod method) {
    // See TestForTransformation.visit(...)
    String name = method.getName();
    if (!name.startsWith("test") ||
        name.indexOf('$') != -1 ||
        !method.getModifierList().hasModifierProperty(PsiModifier.PUBLIC) ||
        method.getModifierList().hasModifierProperty(PsiModifier.ABSTRACT) ||
        method.getParameterList().getParametersCount() != 0) {
      return false;
    }

    PsiClass containingClass = method.getContainingClass();
    if (containingClass == null) return false;

    if (!AnnotationUtil.isAnnotated(containingClass, GrailsTestUtils.TEST_ANNOTATIONS, 0)) {
      return false;
    }

    if (JUnitUtil.isJUnit3TestClass(containingClass)) return false;
    if (InheritanceUtil.isInheritor(containingClass, SpockUtils.SPEC_CLASS_NAME)) return false;

    return true;
  }
}

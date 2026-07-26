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
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.AbstractClosureParameterEnhancer;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public final class GrailsValidatorParamTypeEnhancer extends AbstractClosureParameterEnhancer {

  @Override
  protected PsiType getClosureParameterType(@NotNull GrFunctionalExpression expression, int index) {
    PsiElement eNamedArgument = expression.getParent();
    if (!(eNamedArgument instanceof GrNamedArgument) || !"validator".equals(((GrNamedArgument)eNamedArgument).getLabelName())) return null;

    PsiElement constraintMethodCall = PsiUtil.getCallByNamedParameter((GrNamedArgument)eNamedArgument);
    if (!(constraintMethodCall instanceof GrMethodCall)) return null;

    PsiMethod constraintMethod = ((GrMethodCall)constraintMethodCall).resolveMethod();
    if (!GrailsConstraintsUtil.isConstraintsMethod(constraintMethod)) return null;

    return switch (index) {
      case 0 -> GrailsConstraintsUtil.getValidatedValueType(constraintMethod);
      case 1 -> {
        PsiClass validatedClass = GrailsConstraintsUtil.getValidatedClass(constraintMethod);
        if (validatedClass == null) yield null;
        yield PsiTypesUtil.getClassType(validatedClass);
      }
      case 2 -> TypesUtil.createTypeByFQClassName("org.springframework.validation.Errors", constraintMethodCall);
      default -> null;
    };
  }
}

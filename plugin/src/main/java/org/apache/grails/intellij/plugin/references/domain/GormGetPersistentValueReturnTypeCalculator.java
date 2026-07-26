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

package org.apache.grails.intellij.plugin.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.util.PairFunction;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

public class GormGetPersistentValueReturnTypeCalculator implements PairFunction<GrMethodCall, PsiMethod, PsiType> {
  @Override
  public PsiType fun(GrMethodCall methodCall, PsiMethod method) {
    GrExpression[] arguments = methodCall.getArgumentList().getExpressionArguments();
    if (arguments.length == 0) return null;
    
    if (!(arguments[0] instanceof GrLiteralImpl)) return null;
    
    Object value = ((GrLiteralImpl)arguments[0]).getValue();
    if (!(value instanceof String)) return null;

    PsiClass domainClass = ((GrLightMethodBuilder)method).getData();

    Pair<PsiType,PsiElement> pair = DomainDescriptor.getPersistentProperties(domainClass).get(value);
    if (pair == null) return null;
    
    return pair.first;
  }
}

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
package org.apache.grails.intellij.plugin.references.domain.detachedCriteria;

import com.intellij.psi.PsiType;
import com.intellij.util.ProcessingContext;
import groovy.lang.Closure;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyClosurePattern;
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatternsKt;
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo;
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.GrDelegatesToProvider;

public final class DetachedCriteriaDelegatesToProvider implements GrDelegatesToProvider {

  private static final GroovyClosurePattern CLOSURE_PATTERN = GroovyPatternsKt.groovyClosure().inMethod(
    GroovyPatternsKt.psiMethod(
      "grails.gorm.DetachedCriteria",
      "and", "or", "not", // DetachedCriteria#handleJunction()
      "get", "list", "count", "exists", "find", // DetachedCriteria#withPopulatedQuery()
      "eqAll", "gtAll", "ltAll", "geAll", "leAll", // DetachedCriteria#buildQueryableCriteria()
      "build", "where" // DetachedCriteria#build()
    )
  );

  @Override
  public @Nullable DelegatesToInfo getDelegatesToInfo(@NotNull GrFunctionalExpression expression) {
    ProcessingContext context = new ProcessingContext();
    if (!CLOSURE_PATTERN.accepts(expression, context)) return null;
    GrCall call = context.get(GroovyPatternsKt.getClosureCallKey());
    if (!(call instanceof GrMethodCall methodCall)) return null;
    if (!(methodCall.getInvokedExpression() instanceof GrReferenceExpression referenceExpression)) return null;
    GrExpression qualifier = referenceExpression.getQualifierExpression();
    if (qualifier == null) return null;
    PsiType type = qualifier.getType();
    if (type == null) return null;
    return new DelegatesToInfo(type, Closure.DELEGATE_FIRST);
  }
}

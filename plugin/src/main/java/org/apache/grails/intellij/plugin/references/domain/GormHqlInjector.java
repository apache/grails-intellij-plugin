/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.grails.intellij.plugin.references.domain;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;

import java.util.List;
import java.util.Set;

/**
 * Injects the HQL language into the query string passed to GORM's {@code find}, {@code findAll},
 * {@code executeQuery} and {@code executeUpdate} methods on domain classes, so that class and
 * property names inside the query become real references (navigation, find usages and rename work).
 *
 * <p>This replaces the old IntelliLang XML injection which matched the synthetic
 * {@code Gorm:DomainDescriptor:DynamicMethod} light methods. Those are only generated for GORM
 * below 4 (see {@link GormAstTransformationContributor}); on GORM 4+ the finder methods come from
 * the {@code GormEntity} trait and are not marked, so the XML matcher never fired. Resolving the
 * domain class directly here keeps the behaviour working across all GORM/Grails versions.
 */
public final class GormHqlInjector implements MultiHostInjector {

  private static final String HQL_LANGUAGE_ID = "HQL";

  private static final Set<String> QUERY_METHODS = Set.of("find", "findAll", "executeQuery", "executeUpdate");

  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    GrLiteral literal = (GrLiteral)context;
    if (!literal.isString()) return;

    GrMethodCall call = getQueryCall(literal);
    if (call == null) return;

    GrReferenceExpression invoked = getInvokedReference(call);
    if (invoked == null || !QUERY_METHODS.contains(invoked.getReferenceName())) return;

    PsiClass domainClass = resolveDomainClass(invoked);
    if (!GormUtils.isGormBean(domainClass)) return;

    Language hql = Language.findLanguageByID(HQL_LANGUAGE_ID);
    if (hql == null) return;

    TextRange range = ElementManipulators.getValueTextRange(literal);
    registrar.startInjecting(hql).addPlace(null, null, (GrLiteralImpl)literal, range).doneInjecting();
  }

  /**
   * Returns the method call whose first positional argument is {@code literal}, or {@code null} when
   * {@code literal} is not directly the first argument of a call.
   */
  private static @Nullable GrMethodCall getQueryCall(@NotNull GrLiteral literal) {
    PsiElement parent = literal.getParent();
    if (!(parent instanceof GrArgumentList)) return null;
    if (!(parent.getParent() instanceof GrMethodCall call)) return null;

    GrExpression[] arguments = call.getArgumentList().getExpressionArguments();
    if (arguments.length == 0 || arguments[0] != literal) return null;

    return call;
  }

  private static @Nullable GrReferenceExpression getInvokedReference(@NotNull GrMethodCall call) {
    GrExpression invoked = call.getInvokedExpression();
    return invoked instanceof GrReferenceExpression ref ? ref : null;
  }

  /**
   * Determines the domain class a finder call is invoked on: the qualifier for {@code Book.find(...)},
   * or the enclosing class for an unqualified {@code find(...)} inside a domain class body.
   */
  private static @Nullable PsiClass resolveDomainClass(@NotNull GrReferenceExpression invoked) {
    GrExpression qualifier = invoked.getQualifierExpression();
    if (qualifier == null) {
      return PsiTreeUtil.getParentOfType(invoked, PsiClass.class);
    }

    if (qualifier instanceof GrReferenceExpression qualifierRef && qualifierRef.resolve() instanceof PsiClass psiClass) {
      return psiClass;
    }

    return PsiTypesUtil.getPsiClass(qualifier.getType());
  }

  @Override
  public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return List.of(GrLiteral.class);
  }
}

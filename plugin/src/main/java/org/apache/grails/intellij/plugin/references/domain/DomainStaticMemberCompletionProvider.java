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

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.completion.GroovyCompletionUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

import java.util.ArrayList;
import java.util.List;

/**
 * Contributes the static domain members (e.g. {@code createCriteria}, {@code list},
 * {@code listOrderBy...}) for a static domain-class reference like {@code Domain.<caret>}.
 *
 * <p>Since IntelliJ 2026.2 a bare class-reference qualifier is treated as an instance type during
 * code completion, so the platform filters these static-only members out of the variants even
 * though the members contributor still produces them. Contribute them explicitly here, recovering
 * the domain class from the static qualifier reference (like {@link GormDynamicFinderCompletionProvider}).</p>
 */
public final class DomainStaticMemberCompletionProvider extends CompletionProvider<CompletionParameters> {

  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {
    PsiElement parent = parameters.getPosition().getParent();
    if (!(parent instanceof GrReferenceExpression refExpr)) return;

    GrExpression qualifier = refExpr.getQualifierExpression();
    if (qualifier == null) return;

    PsiReference qualifierRef = qualifier.getReference();
    PsiElement resolved = qualifierRef == null ? null : qualifierRef.resolve();
    if (!(resolved instanceof PsiClass domainClass)) return;
    if (!GormUtils.isGormBean(domainClass)) return;

    List<PsiMethod> collected = new ArrayList<>();
    PsiScopeProcessor processor = new PsiScopeProcessor() {
      @Override
      public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
        if (element instanceof PsiMethod method) collected.add(method);
        return true;
      }

      @Override
      public <T> @Nullable T getHint(@NotNull Key<T> hintKey) {
        return null; // no NameHint: enumerate every static member
      }
    };

    DomainMembersProvider.processStaticMembersForCompletion(refExpr, domainClass, processor);

    for (PsiMethod method : collected) {
      result.addElement(GroovyCompletionUtil.createLookupElement(method));
    }
  }
}

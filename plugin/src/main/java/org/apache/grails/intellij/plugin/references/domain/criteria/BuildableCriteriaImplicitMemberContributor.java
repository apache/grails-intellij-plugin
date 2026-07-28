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

package org.apache.grails.intellij.plugin.references.domain.criteria;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.DelegatingScopeProcessor;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

import java.util.Set;

/**
 * Since GORM 4 {@code createCriteria()} comes from the {@code GormEntity} trait and is declared to return
 * {@link #BUILDABLE_CRITERIA_CLASS}, not {@link CriteriaBuilderUtil#CRITERIA_BUILDER_CLASS}. That interface
 * declares {@code get}, {@code list}, {@code listDistinct} and {@code scroll}, which is why those forms
 * resolve out of the box, but the remaining closure-terminal calls only exist in
 * {@code AbstractHibernateCriteriaBuilder.invokeMethod(...)} and are therefore invisible to resolution:
 * {@code Ddd.createCriteria().count { ... }} used to resolve to nothing, which in turn left
 * {@link CriteriaBuilderUtil#checkCriteriaClosure} unable to find the domain class, so no property inside
 * the closure could be navigated either.
 */
final class BuildableCriteriaImplicitMemberContributor extends NonCodeMembersContributor {
  public static final String BUILDABLE_CRITERIA_CLASS = "org.grails.datastore.mapping.query.api.BuildableCriteria";

  /**
   * Members of {@link CriteriaBuilderImplicitMemberContributor#CLASS_SOURCE} that
   * {@link #BUILDABLE_CRITERIA_CLASS} (or its {@code Criteria} supertype) does not declare itself.
   * Contributing the rest would duplicate real methods.
   */
  // #CHECK# org.grails.datastore.mapping.query.api.BuildableCriteria against org.grails.orm.hibernate.query.AbstractHibernateCriteriaBuilder#invokeMethod(...)
  private static final Set<String> MEMBERS_MISSING_FROM_BUILDABLE_CRITERIA = Set.of("count", "call", "doCall");

  @Override
  protected String getParentClassName() {
    return BUILDABLE_CRITERIA_CLASS;
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (aClass == null) return;

    // HibernateCriteriaBuilder implements BuildableCriteria since GORM 4, and a qualifier typed as the
    // builder is already served by CriteriaBuilderImplicitMemberContributor.
    if (InheritanceUtil.isInheritor(aClass, CriteriaBuilderUtil.CRITERIA_BUILDER_CLASS)) return;

    String nameHint = ResolveUtil.getNameHint(processor);
    if (nameHint != null && !MEMBERS_MISSING_FROM_BUILDABLE_CRITERIA.contains(nameHint)) return;

    // Same class source as CriteriaBuilderImplicitMemberContributor, so that the contributed methods pass
    // CriteriaBuilderImplicitMemberContributor.isMine() and keep working with CriteriaBuilderUtil and
    // CriteriaReturnTypeCalculator.
    DynamicMemberUtils.process(new DelegatingScopeProcessor(processor) {
      @Override
      public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
        if (element instanceof PsiMethod method && !MEMBERS_MISSING_FROM_BUILDABLE_CRITERIA.contains(method.getName())) {
          return true;
        }

        return super.execute(element, state);
      }
    }, false, place, CriteriaBuilderImplicitMemberContributor.CLASS_SOURCE);
  }
}

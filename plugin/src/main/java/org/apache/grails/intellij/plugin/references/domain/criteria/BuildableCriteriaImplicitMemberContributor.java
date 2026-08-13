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
import org.apache.grails.intellij.plugin.gorm.GormClassNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

import java.util.Set;

/**
 * From GORM 4 on, {@code createCriteria()} comes from the {@code GormEntity} trait and is declared to return
 * {@code BuildableCriteria}, not {@code HibernateCriteriaBuilder}. That interface declares {@code get},
 * {@code list}, {@code listDistinct} and {@code scroll}, which is why those forms resolve out of the box,
 * but the remaining closure-terminal calls only exist in
 * {@code AbstractHibernateCriteriaBuilder.invokeMethod(...)} and are therefore invisible to resolution:
 * {@code Ddd.createCriteria().count { ... }} used to resolve to nothing, which in turn left
 * {@link CriteriaBuilderUtil#checkCriteriaClosure} unable to find the domain class, so no property inside
 * the closure could be navigated either.
 */
final class BuildableCriteriaImplicitMemberContributor extends NonCodeMembersContributor {

  /**
   * The qualifier-level terminal calls a {@code BuildableCriteria}-typed qualifier accepts but that neither
   * it nor its {@code Criteria} supertype declares, so resolution cannot see them without help.
   *
   * <p>This is deliberately narrower than "every member of
   * {@link CriteriaBuilderImplicitMemberContributor#CLASS_SOURCE} the interface does not declare":
   * closure-body members such as {@code projections} are missing from both interfaces too, but they are
   * supplied inside the closure by {@link CriteriaClosureMemberContributor}, and contributing them here
   * would put a bogus entry on every {@code createCriteria()} qualifier instead. Contributing a member the
   * interface does declare would duplicate the real method.
   *
   * <p>Of the {@code def c = Ddd.createCriteria(); c { ... }} shorthand, only {@code call} is load-bearing:
   * that is what Groovy's implicit-call resolution looks for. {@code doCall} is closure protocol and is kept
   * only for parity with {@code CLASS_SOURCE}.
   */
  // #CHECK# org.grails.datastore.mapping.query.api.BuildableCriteria against org.grails.orm.hibernate.query.AbstractHibernateCriteriaBuilder#invokeMethod(...)
  private static final Set<String> MEMBERS_MISSING_FROM_BUILDABLE_CRITERIA = Set.of("count", "call", "doCall");

  @Override
  protected String getParentClassName() {
    return GormClassNames.BUILDABLE_CRITERIA;
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (aClass == null) return;

    String nameHint = ResolveUtil.getNameHint(processor);
    if (nameHint != null && !MEMBERS_MISSING_FROM_BUILDABLE_CRITERIA.contains(nameHint)) return;

    // HibernateCriteriaBuilder implements BuildableCriteria since GORM 4, and a qualifier typed as the
    // builder is already served by CriteriaBuilderImplicitMemberContributor.
    if (InheritanceUtil.isInheritor(aClass, CriteriaBuilderUtil.CRITERIA_BUILDER_CLASS)) return;

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

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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.DelegatingScopeProcessor;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrMethodWrapper;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class DetachedCriteriaClosureMemberProvider extends ClosureMemberContributor {
  public static final Object MARKER = new Object();
  public static final Object PROJECTION_METHOD_MARKER = new Object();

  @Override
  protected void processMembers(@NotNull GrClosableBlock closure, @NotNull PsiScopeProcessor processor, @NotNull PsiElement place, @NotNull ResolveState state) {
    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return;

    if (closure != PsiTreeUtil.getParentOfType(place, GrClosableBlock.class)) return;

    PsiElement parent = closure.getParent();
    if (parent instanceof GrArgumentList) parent = parent.getParent();

    if (!(parent instanceof GrMethodCall)) return;

    PsiMethod method = ((GrMethodCall)parent).resolveMethod();
    if (method == null) return;

    PsiClass domainClass = null;

    if (DetachedCriteriaUtil.isDomainDetachedCriteriaMethod(method) || GrLightMethodBuilder.checkKind(method, MARKER)) {
      domainClass = ((GrLightMethodBuilder)method).getData();
      final PsiClass finalDomainClass = domainClass;

      if (!GrailsPsiUtil.process(DetachedCriteriaUtil.DETACHED_CRITERIA_CLASS, new DelegatingScopeProcessor(processor) {
        @Override
        public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
          if (element instanceof PsiMethod) {
            GrLightMethodBuilder res = GrMethodWrapper.wrap((PsiMethod)element);
            res.setMethodKind(MARKER);
            res.setData(finalDomainClass);
            return super.execute(res, state);
          }

          return true;
        }
      }, place, state)) return;
    }

    if ("projections".equals(method.getName())) {
      if (domainClass == null) {
        PsiClass containingClass = method.getContainingClass();
        if (containingClass == null || !DetachedCriteriaUtil.DETACHED_CRITERIA_CLASS.equals(containingClass.getQualifiedName())) return;

        domainClass = DetachedCriteriaUtil.getDomainFromSubstitutor(((GrMethodCall)parent).advancedResolve());
        if (domainClass == null) return;
      }

      final PsiClass finalDomainClass1 = domainClass;

      GrailsPsiUtil.process("grails.gorm.DetachedCriteria.DetachedProjections", new DelegatingScopeProcessor(processor) {
          @Override
          public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
            if (element instanceof PsiMethod) {
              GrLightMethodBuilder res = GrMethodWrapper.wrap((PsiMethod)element);
              res.setMethodKind(PROJECTION_METHOD_MARKER);
              res.setData(finalDomainClass1);
              return super.execute(res, state);
            }

            return true;
          }
      }, place, state);
    }
  }
}

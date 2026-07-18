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

package org.jetbrains.plugins.grails.references.domain.criteria;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.references.domain.DomainDescriptor;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.Map;

final class CriteriaClosureMemberContributor extends ClosureMemberContributor {
  public static final Object TO_MANY_RELATIONSHIP_MARKER = new Object();

  @Override
  protected void processMembers(@NotNull GrClosableBlock closure, @NotNull PsiScopeProcessor processor, @NotNull PsiElement place, @NotNull ResolveState state) {
    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return;

    if (closure != PsiTreeUtil.getParentOfType(place, GrClosableBlock.class)) return;

    PsiClass domainClass = CriteriaBuilderUtil.checkCriteriaClosure(closure);
    if (domainClass == null) return;

    PsiClass hibCriteriaBuilder = JavaPsiFacade.getInstance(domainClass.getProject()).findClass(CriteriaBuilderUtil.CRITERIA_BUILDER_CLASS, place.getResolveScope());
    if (hibCriteriaBuilder != null) {
      if (!hibCriteriaBuilder.processDeclarations(processor, state, null, place)) return;

      if (!CriteriaBuilderImplicitMemberContributor.process(processor, hibCriteriaBuilder, place, state)) return;
    }

    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(domainClass);

    String nameHint = ResolveUtil.getNameHint(processor);

    if (nameHint == null) {
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : descriptor.getHasMany().entrySet()) {
        GrLightMethodBuilder method = createMethod(entry.getKey(), entry.getValue());
        if (method != null) {
          if (!processor.execute(method, state)) return;
        }
      }
    }
    else {
      Pair<PsiType, PsiElement> pair = descriptor.getHasMany().get(nameHint);
      if (pair != null) {
        GrLightMethodBuilder method = createMethod(nameHint, pair);
        if (method != null) {
          if (!processor.execute(method, state)) return;
        }
      }
    }
  }

  private static @Nullable GrLightMethodBuilder createMethod(String propertyName, Pair<PsiType, PsiElement> pair) {
    PsiClass domainClass = PsiTypesUtil.getPsiClass(pair.first);
    if (!GormUtils.isGormBean(domainClass)) return null;

    @SuppressWarnings("ConstantConditions")
    GrLightMethodBuilder builder = new GrLightMethodBuilder(domainClass.getManager(), propertyName);
    builder.addParameter("closure", GroovyCommonClassNames.GROOVY_LANG_CLOSURE);
    builder.setMethodKind(TO_MANY_RELATIONSHIP_MARKER);
    builder.setData(domainClass);
    builder.setContainingClass(domainClass);

    builder.setNavigationElement(pair.second);

    return builder;
  }
}

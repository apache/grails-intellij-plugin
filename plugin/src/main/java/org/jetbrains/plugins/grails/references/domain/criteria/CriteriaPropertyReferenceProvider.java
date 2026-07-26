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

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.GormPropertyReference;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

public class CriteriaPropertyReferenceProvider extends GrailsMethodNamedArgumentReferenceProvider.Contributor.Provider implements GrailsMethodNamedArgumentReferenceProvider.Contributor {

  @Override
  public void register(GrailsMethodNamedArgumentReferenceProvider registrar) {
    // Not ClassNameCondition/ClassSourceCondition: several criteria DSL methods (eq, order, ...) are
    // declared on a supertype/interface implemented by HibernateCriteriaBuilder (e.g.
    // org.grails.datastore.mapping.query.api.Criteria) rather than on HibernateCriteriaBuilder itself,
    // so an exact-class-name match misses them. CriteriaBuilderUtil.isCriteriaBuilderMethod accounts
    // for that inheritance.
    Condition<PsiMethod> condition = CriteriaBuilderUtil::isCriteriaBuilderMethod;

    registrar.register(0, this, condition,
                       // #CHECK# grails.orm.HibernateCriteriaBuilder
                       "property", "distinct", "avg", "calculatePropertyName", "count", "countDistinct", "groupProperty", "max", "min",
                       "sum", "gt", "ge", "lt", "le", "eq", "like", "rlike", "ilike", "in", "inList", "order", "sizeEq", "sizeGt", "sizeGe",
                       "sizeLe", "sizeLt", "sizeNe", "ne", "notEqual", "between", "fetchMode", "createAlias",

                           // From HibernateCriteriaBuilder.invokeMethod(...)
                       "isNull", "isNotNull", "isEmpty", "isNotEmpty");

    String[] twoArgumentMethods = {"eqProperty", "neProperty", "gtProperty", "geProperty", "ltProperty", "leProperty"};
    registrar.register(0, this, condition, twoArgumentMethods);
    registrar.register(1, this, condition, twoArgumentMethods);
  }

  @Override
  public PsiReference[] createRef(@NotNull PsiElement element,
                                  @NotNull GrMethodCall methodCall,
                                  int argumentIndex,
                                  @NotNull GroovyResolveResult resolveResult) {
    PsiClass domainClass = CriteriaBuilderUtil.findDomainClassByMethodCall(methodCall, false);
    if (domainClass == null) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{new GormPropertyReference(element, false, domainClass)};
  }

}

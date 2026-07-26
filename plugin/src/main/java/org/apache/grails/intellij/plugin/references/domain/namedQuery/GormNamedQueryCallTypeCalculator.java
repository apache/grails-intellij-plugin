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

package org.apache.grails.intellij.plugin.references.domain.namedQuery;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PairFunction;
import org.apache.grails.intellij.plugin.references.domain.criteria.CriteriaBuilderUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public class GormNamedQueryCallTypeCalculator implements PairFunction<GrMethodCall, PsiMethod, PsiType> {
  @Override
  public PsiType fun(GrMethodCall methodCall, PsiMethod method) {
    GrExpression[] allArguments = PsiUtil.getAllArguments(methodCall);

    GrClosableBlock closure = null;
    for (int i = allArguments.length; --i >= 0; ) {
      if (allArguments[i] instanceof GrClosableBlock) {
        closure = (GrClosableBlock)allArguments[i];
        break;
      }
    }

    if (closure == null) return null;

    NamedQueryDescriptor queryDescriptor = ((GrLightMethodBuilder)method).getData();

    PsiClass domainClass = queryDescriptor.getDomainClass();

    PsiType resultElementType = CriteriaBuilderUtil.getResultType(domainClass, closure);

    JavaPsiFacade facade = JavaPsiFacade.getInstance(domainClass.getProject());

    GlobalSearchScope resolveScope = domainClass.getResolveScope();
    PsiClass listClass = facade.findClass(CommonClassNames.JAVA_UTIL_LIST, resolveScope);
    if (listClass == null) {
      return facade.getElementFactory().createTypeByFQClassName(CommonClassNames.JAVA_UTIL_LIST, resolveScope);
    }

    return facade.getElementFactory().createType(listClass, resultElementType);
  }
}

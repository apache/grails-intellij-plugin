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
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.util.PairFunction;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.domain.GormUtils;
import org.apache.grails.intellij.plugin.references.domain.criteria.CriteriaBuilderUtil;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;

import java.util.Map;

public class NamedCriteriaProxyTypeCalculator implements PairFunction<GrMethodCall, PsiMethod, PsiType> {

  private enum ResultType {
    LIST, SINGLE, INTEGER, HAS_PARAMETER_UNIQUE
  }

  private static final Map<String, ResultType> METHODS = GrailsUtils.createMap(
    "list", ResultType.LIST,
    "listDistinct", ResultType.LIST,
    "get", ResultType.SINGLE,
    "findWhere", ResultType.SINGLE,
    "findAllWhere", ResultType.HAS_PARAMETER_UNIQUE,
    "count", ResultType.INTEGER
  );

  @Override
  public PsiType fun(@NotNull GrMethodCall callExpression, @NotNull PsiMethod method) {
    String methodName = method.getName();

    ResultType resultType = METHODS.get(methodName);
    if (resultType == null) return null;

    NamedQueryDescriptor queryDescriptor = GormUtils.getQueryDescriptorByProxyMethod(callExpression);
    if (queryDescriptor == null) return null;

    PsiClass domainClass = queryDescriptor.getDomainClass();

    JavaPsiFacade facade = JavaPsiFacade.getInstance(domainClass.getProject());
    PsiElementFactory factory = facade.getElementFactory();

    if (resultType == ResultType.INTEGER) {
      return factory.createTypeByFQClassName(CommonClassNames.JAVA_LANG_INTEGER, domainClass.getResolveScope());
    }

    PsiType elementType = CriteriaBuilderUtil.getResultType(domainClass, queryDescriptor.getClosure());

    if (resultType == ResultType.SINGLE) {
      return elementType;
    }

    if (resultType == ResultType.HAS_PARAMETER_UNIQUE) {
      GrArgumentList argumentList = callExpression.getArgumentList();
      GrExpression[] arguments = argumentList.getExpressionArguments();
      if (arguments.length > 0) {
        GrExpression e = arguments[arguments.length - 1];
        if (e instanceof GrLiteralImpl) {
          if (((GrLiteralImpl)e).getValue() == Boolean.TRUE) {
            return elementType;
          }
        }
      }
    }

    PsiClass listClass = facade.findClass(CommonClassNames.JAVA_UTIL_LIST, domainClass.getResolveScope());
    if (listClass == null) return null;

    return factory.createType(listClass, elementType);
  }
}

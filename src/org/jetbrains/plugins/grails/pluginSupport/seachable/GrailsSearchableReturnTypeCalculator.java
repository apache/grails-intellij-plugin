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

package org.jetbrains.plugins.grails.pluginSupport.seachable;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElementFactory;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiParameter;
import com.intellij.psi.PsiType;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.PairFunction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;
import org.jetbrains.plugins.groovy.lang.psi.impl.GrMapType;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

import java.util.Collections;
import java.util.LinkedHashMap;

public class GrailsSearchableReturnTypeCalculator implements PairFunction<GrMethodCall, PsiMethod, PsiType> {

  @Override
  public PsiType fun(@NotNull GrMethodCall callExpression, @NotNull PsiMethod method) {
    boolean isFromProvider = GrLightMethodBuilder.checkKind(method, GrailsSearchableMemberProvider.METHOD_MARKER);
    if (!isFromProvider) {
      if (!GrailsArtifact.SERVICE.isInstance(method.getContainingClass())) {
        return null;
      }
    }

    String name = method.getName();
    if (!"search".equals(name) && !"moreLikeThis".equals(name)) return null;

    GrNamedArgument[] options = null;

    GrArgumentList argumentList = callExpression.getArgumentList();
    GrNamedArgument[] namedArguments = argumentList.getNamedArguments();
    if (namedArguments.length == 0) {
      for (GrExpression expression : argumentList.getExpressionArguments()) {
        if (expression instanceof GrListOrMap) {
          options = ((GrListOrMap)expression).getNamedArguments();
          break;
        }
      }
    }
    else {
      options = namedArguments;
    }

    if (options == null && isFromProvider) {
      for (PsiParameter parameter : method.getParameterList().getParameters()) {
        if (parameter.getType().equalsToText(CommonClassNames.JAVA_UTIL_MAP)) {
          return null; // Unknown map passed as options. We can't determinate return type.
        }
      }
    }

    String resultType = null;

    if (options != null) {
      for (GrNamedArgument namedArgument : options) {
        if ("result".equals(namedArgument.getLabelName())) {
          GrExpression expression = namedArgument.getExpression();
          if (!(expression instanceof GrLiteral l)) return null; // We can't determinate return type.
          if (!l.isString()) return null; // We can't determinate return type.
          resultType = (String)l.getValue();
          break;
        }
      }
    }

    if ("count".equals(resultType)) {
      return TypesUtil.createType(CommonClassNames.JAVA_LANG_INTEGER, callExpression);
    }

    PsiClass domainClass = null;
    if (method instanceof GrLightMethodBuilder) {
      domainClass = ((GrLightMethodBuilder)method).getData();
    }

    if (resultType == null || resultType.equals("searchResult")) {
      JavaPsiFacade facade = JavaPsiFacade.getInstance(method.getProject());
      PsiElementFactory factory = facade.getElementFactory();
      GlobalSearchScope resolveScope = callExpression.getResolveScope();

      LinkedHashMap<String, PsiType> map = new LinkedHashMap<>();

      PsiClass listClass = facade.findClass(CommonClassNames.JAVA_UTIL_LIST, resolveScope);
      if (listClass == null) return null;

      PsiType resultList;
      if (domainClass == null) {
        resultList = factory.createType(listClass);
      }
      else {
        resultList = factory.createType(listClass, factory.createType(domainClass));
      }
      map.put("results", resultList);

      PsiClassType floatType = factory.createTypeByFQClassName(CommonClassNames.JAVA_LANG_FLOAT, resolveScope);
      map.put("scores", factory.createType(listClass, floatType));

      PsiClassType integerType = factory.createTypeByFQClassName(CommonClassNames.JAVA_LANG_INTEGER, resolveScope);

      map.put("total", integerType);

      map.put("offset", integerType);
      map.put("max", integerType);

      map.put("suggestedQuery", factory.createTypeByFQClassName(CommonClassNames.JAVA_LANG_STRING, resolveScope));

      return GrMapType.create(facade, resolveScope, map, Collections.emptyList());
    }
    else if (resultType.equals("every")) {
      if (domainClass == null) {
        return TypesUtil.createType(CommonClassNames.JAVA_UTIL_LIST, callExpression);
      }
      else {
        return TypesUtil.createListType(domainClass);
      }
    }
    else if (resultType.equals("top")) {
      if (domainClass == null) return null;

      return PsiTypesUtil.getClassType(domainClass);
    }

    return null;
  }

}

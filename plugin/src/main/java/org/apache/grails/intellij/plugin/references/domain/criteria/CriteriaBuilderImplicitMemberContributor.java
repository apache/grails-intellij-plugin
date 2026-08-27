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

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.DelegatingScopeProcessor;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.util.GrailsNameUtils;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.lang.resolve.imports.GroovyImports;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

final class CriteriaBuilderImplicitMemberContributor extends NonCodeMembersContributor {
  // #CHECK# grails.orm.HibernateCriteriaBuilder#invokeMethod(...)
  public static final String CLASS_SOURCE = """
    class CriteriaBuilderMembers {  public Integer count(groovy.lang.Closure closure)
      public List list(groovy.lang.Closure closure)
      public List list(Map args, groovy.lang.Closure closure)
      public List listDistinct(groovy.lang.Closure closure)
      public List call(groovy.lang.Closure closure)
      public List doCall(groovy.lang.Closure closure)
      public Object get(groovy.lang.Closure closure)
      public org.hibernate.ScrollableResults scroll(groovy.lang.Closure closure)
      public String and(groovy.lang.Closure closure)
      public String or(groovy.lang.Closure closure)
      public String not(groovy.lang.Closure closure)
      public String projections(groovy.lang.Closure closure)
      public org.hibernate.criterion.Criterion idEq(Object value)
      public org.hibernate.criterion.Criterion isNull(String propertyName)
      public org.hibernate.criterion.Criterion isNotNull(String propertyName)
      public org.hibernate.criterion.Criterion isEmpty(String propertyName)
      public org.hibernate.criterion.Criterion isNotEmpty(String propertyName)
    }""";

  @Override
  protected String getParentClassName() {
    return CriteriaBuilderUtil.CRITERIA_BUILDER_CLASS;
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (aClass == null) return;
    process(processor, aClass, place, state);
  }

  public static boolean process(@NotNull PsiScopeProcessor processor,
                                @NotNull PsiClass hibernateCriteriaBuilderClass,
                                @NotNull PsiElement place,
                                @NotNull ResolveState state) {
    if (!DynamicMemberUtils.process(processor, false, place, CLASS_SOURCE)) return false;

    PsiClass criteriaClass = JavaPsiFacade.getInstance(place.getProject()).findClass("org.hibernate.Criteria", place.getResolveScope());
    if (criteriaClass != null) {
      if (!criteriaClass.processDeclarations(new DelegatingScopeProcessor(processor) {
        @Override
        public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
          if (element instanceof PsiMethod && "list".equals(((PsiMethod)element).getName())) return true; // Filter 'Criteria.list()' method.

          return super.execute(element, state);
        }
      }, state, null, place)) return false;

      if (ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) {
        String nameHint = ResolveUtil.getNameHint(processor);
        if (nameHint != null && !nameHint.isEmpty()) {
          String setterName = GrailsNameUtils.getSetterName(nameHint);

          for (PsiMethod method : criteriaClass.findMethodsByName(setterName, false)) {
            if (hibernateCriteriaBuilderClass.findMethodsByName(nameHint, true).length == 0) { // method 'fetchMode()' exists in HibernateCriteriaBuilder.
              // hack to pass name check in org.jetbrains.plugins.groovy.lang.resolve.processors.GroovyResolverProcessor
              ResolveState newState = state.put(GroovyImports.getImportedNameKey(), nameHint);
              if (!processor.execute(method, newState)) return false;
            }
          }
        }
      }
    }

    return true;
  }

  public static boolean isMine(@Nullable PsiMethod method) {
    return DynamicMemberUtils.isDynamicElement(method, CLASS_SOURCE);
  }
}

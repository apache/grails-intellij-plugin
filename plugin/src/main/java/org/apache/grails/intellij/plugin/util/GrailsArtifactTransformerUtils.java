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

package org.apache.grails.intellij.plugin.util;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifierFlags;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.params.GrParameter;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightParameterListBuilder;
import org.jetbrains.plugins.groovy.lang.typing.PredefinedReturnType;
import org.jetbrains.plugins.groovy.transformations.TransformationContext;

import java.util.Collection;

/**
 * See org.codehaus.groovy.grails.compiler.injection.AbstractGrailsArtefactTransformer
 */
public final class GrailsArtifactTransformerUtils {

  private GrailsArtifactTransformerUtils() {

  }

  /**
   * See GrailsASTUtils.isCandidateMethod()
   */
  public static boolean isCandidateMethod(PsiMethod method) {
    return method.getName().indexOf('$') == -1 &&
           method.hasModifierProperty(PsiModifier.PUBLIC) &&
           !method.hasModifierProperty(PsiModifier.ABSTRACT) &&
           !method.hasModifierProperty(PsiModifier.STATIC);
  }

  public static void enhanceAst(@NotNull PsiClass apiClass,
                                @NotNull TransformationContext context,
                                Collection<? super PsiMethod> results,
                                boolean isStatic,
                                @NotNull Condition<? super PsiMethod> methodFilter) {
    PsiSubstitutor returnTypeSubstitutor = null;
    PsiSubstitutor substitutor = PsiSubstitutor.EMPTY;
    PsiTypeParameter[] typeParameters = apiClass.getTypeParameters();
    if (typeParameters.length == 1) {
      PsiClassType enhancedClassType = context.getClassType();
      if (isStatic) {
        returnTypeSubstitutor = PsiSubstitutor.EMPTY.put(typeParameters[0], enhancedClassType);
        substitutor = PsiSubstitutor.EMPTY.put(typeParameters[0], null);
      }
      else {
        substitutor = PsiSubstitutor.EMPTY.put(typeParameters[0], enhancedClassType);
      }
    }


    for (PsiMethod method : apiClass.getAllMethods()) {
      if (method.isConstructor()) continue;
      PsiClass containingClass = method.getContainingClass();
      if (containingClass == null) continue;

      String qName = containingClass.getQualifiedName();
      if (qName == null || qName.startsWith("java.") || qName.startsWith("groovy.")) continue;

      if (!methodFilter.value(method)) continue;

      // See GrailsASTUtils.addDelegateStaticMethod() and GrailsASTUtils.addDelegateInstanceMethod()

      GrLightMethodBuilder m = GrailsPsiUtil.substitute(method, substitutor);

      if (isStatic) {
        m.addModifier(GrModifierFlags.STATIC_MASK);

        PsiType methodReturnType = method.getReturnType();
        if (methodReturnType != null) {
          if (returnTypeSubstitutor != null) {
            PsiType realReturnType = returnTypeSubstitutor.substitute(methodReturnType);
            if (!methodReturnType.equals(realReturnType)) {
              m.putUserData(PredefinedReturnType.PREDEFINED_RETURN_TYPE_KEY, realReturnType);
            }
          }
        }
      }
      else {
        GrLightParameterListBuilder parameterList = m.getParameterList();
        if (parameterList.getParametersCount() == 0) continue;
        GrParameter parameter = parameterList.removeParameter(0);
        PsiType parameterType = parameter.getType();
        if (!(parameterType instanceof PsiClassType)) continue;
        var parameterClass = ((PsiClassType)parameterType).resolve();
        if (parameterClass == null || !context.isInheritor(parameterClass)) continue;
      }

      GrTypeDefinition codeClass = context.getCodeClass();
      m.setData(codeClass);

      if (codeClass.findCodeMethodsBySignature(m, false).length == 0) { // Don't search in parent
        results.add(m);
      }
    }
  }

  public static class DefaultFilter implements Condition<PsiMethod> {
    public static final DefaultFilter INSTANCE = new DefaultFilter();

    @Override
    public boolean value(PsiMethod method) {
      return isCandidateMethod(method);
    }
  }

}

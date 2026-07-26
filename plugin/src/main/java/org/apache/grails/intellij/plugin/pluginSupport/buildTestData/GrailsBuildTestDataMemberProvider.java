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

package org.apache.grails.intellij.plugin.pluginSupport.buildTestData;

import com.intellij.psi.PsiAnnotation;
import com.intellij.psi.PsiAnnotationMemberValue;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifierList;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.DelegatingScopeProcessor;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.MemberProvider;
import org.apache.grails.intellij.plugin.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationArrayInitializer;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.annotation.GrAnnotationMemberValue;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

public class GrailsBuildTestDataMemberProvider extends MemberProvider {

  public static final Object METHOD_MARKER = "grails:plugins:buildTestData";

  public static final String BUILD_ANNOTATION = "grails.buildtestdata.mixin.Build";

  private static final String CLASS_SOURCE = """
    /** @originalInfo provided by 'build-test-data' plugin */
    class BuildTestDataPluginMethods<D> {
      public static D build(Map propValues = [:]) {}
      public static D buildLazy(Map propValues = [:]) {}
      public static D buildWithoutSave(Map propValues = [:]) {}
      public static D build(Map args, grails.buildtestdata.CircularCheckList circularCheckList) {}
      public static D buildWithoutSave(Map args, grails.buildtestdata.CircularCheckList circularCheckList) {}
      public static D buildCascadingSave(List circularCheckList) {}
    }""";

  @Override
  public void processMembers(PsiScopeProcessor processor, final PsiClass domainClass, PsiElement place) {
    GrMethod method = PsiTreeUtil.getParentOfType(place, GrMethod.class);
    if (method == null) return;

    PsiClass testClass = method.getContainingClass();
    if (testClass == null) return;

    PsiModifierList modifierList = testClass.getModifierList();
    if (modifierList == null) return;

    PsiAnnotation buildAnnotation = modifierList.findAnnotation(BUILD_ANNOTATION);
    if (buildAnnotation == null) {
      return;
    }

    if (!isSupport(buildAnnotation, domainClass)) return;

    DelegatingScopeProcessor delegateProcessor = new DelegatingScopeProcessor(processor) {

      private PsiSubstitutor mySubstitutor;

      @Override
      public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
        if (!(element instanceof PsiMethod)) return true;

        if (mySubstitutor == null) {
          mySubstitutor = PsiSubstitutor.EMPTY.putAll(((DynamicMemberUtils.DynamicElement)element).getSourceClass(), new PsiType[]{
            PsiTypesUtil.getClassType(domainClass)});
        }

        GrLightMethodBuilder lightMethod = GrailsPsiUtil.substitute((PsiMethod)element, mySubstitutor);

        lightMethod.setMethodKind(METHOD_MARKER);

        lightMethod.setData(domainClass);

        return super.execute(lightMethod, state);
      }
    };

    DynamicMemberUtils.process(delegateProcessor, true, place, CLASS_SOURCE);
  }

  private static boolean isSupport(PsiAnnotation buildAnnotation, PsiClass aClass) {
    PsiAnnotationMemberValue value = buildAnnotation.findAttributeValue("value");
    if (value instanceof GrAnnotationArrayInitializer) {
      GrAnnotationMemberValue[] initializers = ((GrAnnotationArrayInitializer)value).getInitializers();

      for (GrAnnotationMemberValue initializer : initializers) {
        if (initializer instanceof GrReferenceExpression) {
          if (((GrReferenceExpression)initializer).resolve() == aClass) {
            return true;
          }
        }
      }

      return false;
    }

    if (value instanceof GrReferenceExpression) {
      if (((GrReferenceExpression)value).resolve() == aClass) {
        return true;
      }
    }

    return false;
  }
}

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

package org.apache.grails.intellij.plugin.references.taglib;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.resolve.taglib.GspTagLibUtil;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.AbstractClosureParameterEnhancer;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;

/**
 * @author user
 */
public final class TaglibClosureParamEnhancer extends AbstractClosureParameterEnhancer {
  @Override
  protected PsiType getClosureParameterType(@NotNull GrFunctionalExpression expression, int index) {
    if (isTaglibClosure(expression)) {
      if (index == 0) {
        return JavaPsiFacade.getElementFactory(expression.getProject()).createTypeByFQClassName(CommonClassNames.JAVA_UTIL_MAP, expression.getResolveScope());
      }

      if (index == 1) {
        return JavaPsiFacade.getElementFactory(expression.getProject()).createTypeByFQClassName(GroovyCommonClassNames.GROOVY_LANG_CLOSURE, expression.getResolveScope());
      }
    }

    return null;
  }

  private static boolean isTaglibClosure(@NotNull GrFunctionalExpression expression) {
    PsiElement parent = expression.getParent();
    if (!(parent instanceof GrField field)) return false;

    PsiClass aClass = field.getContainingClass();
    if (aClass == null) return false;

    String className = aClass.getQualifiedName();
    if (className == null || !className.endsWith(GrailsArtifact.TAGLIB.suffix)) return false;

    if (className.startsWith(GspTagLibUtil.DYNAMIC_TAGLIB_PACKAGE)) {
      return true;
    }

    return GrailsArtifact.TAGLIB.isInstance(aClass);
  }
}

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

package org.apache.grails.intellij.plugin.references.pluginClass;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.typeEnhancers.AbstractClosureParameterEnhancer;

import java.util.HashMap;
import java.util.Map;

/**
 * @author user
 */
public final class GrailsPluginParameterEnhancer extends AbstractClosureParameterEnhancer {

  private static final Map<String, String> TYPE_MAP = new HashMap<>();
  static {
    TYPE_MAP.put("doWithDynamicMethods", "org.springframework.context.ApplicationContext");
    TYPE_MAP.put("doWithApplicationContext", "org.springframework.context.ApplicationContext");
    TYPE_MAP.put("onChange", CommonClassNames.JAVA_UTIL_MAP);
    TYPE_MAP.put("onShutdownListener", CommonClassNames.JAVA_UTIL_MAP);
    TYPE_MAP.put("onConfigChangeListener", CommonClassNames.JAVA_UTIL_MAP);
  }

  @Override
  protected PsiType getClosureParameterType(@NotNull GrFunctionalExpression expression, int index) {
    PsiElement parent = expression.getParent();

    if (!(parent instanceof GrField)) return null;

    PsiClass containingClass = ((GrField)parent).getContainingClass();
    if (!GrailsUtils.isGrailsPluginClass(containingClass)) return null;
    assert containingClass != null;

    String type = TYPE_MAP.get(((GrField)parent).getName());
    if (type == null) return null;

    return JavaPsiFacade.getElementFactory(containingClass.getProject()).createTypeByFQClassName(type, containingClass.getResolveScope());
  }
}

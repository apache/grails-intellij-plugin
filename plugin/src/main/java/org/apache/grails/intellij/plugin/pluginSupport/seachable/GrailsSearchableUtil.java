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

package org.apache.grails.intellij.plugin.pluginSupport.seachable;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

public final class GrailsSearchableUtil {

  private GrailsSearchableUtil() {
  }

  public static PsiMethod createAllMethod(PsiManager manager) {
    GrLightMethodBuilder res = new GrLightMethodBuilder(manager, "all");
    res.addOptionalParameter("args", CommonClassNames.JAVA_UTIL_MAP);
    return res;
  }

  public static PsiMethod createMethod(String name, PsiElement navigationElement, PsiClass containingClass) {
    GrLightMethodBuilder res = new GrLightMethodBuilder(navigationElement.getManager(), name);
    res.addOptionalParameter("args", CommonClassNames.JAVA_UTIL_MAP);
    res.setNavigationElement(navigationElement);
    res.setContainingClass(containingClass);
    return res;
  }

  public static boolean isSearchableField(@NotNull GrField field) {
    if (!"searchable".equals(field.getName()) || !field.hasModifierProperty(PsiModifier.STATIC)) {
      return false;
    }

    return GormUtils.isGormBean(field.getContainingClass());
  }

}

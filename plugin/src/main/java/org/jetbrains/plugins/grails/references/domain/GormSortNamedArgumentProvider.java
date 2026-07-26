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

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.extensions.impl.StringTypeCondition;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;

import java.util.Map;

public class GormSortNamedArgumentProvider extends GroovyNamedArgumentProvider {
  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    GrField field = PsiTreeUtil.getParentOfType(call, GrField.class);
    if (field == null || !"mapping".equals(field.getName()) || !field.hasModifierProperty(PsiModifier.STATIC)) return;

    PsiClass domainClass = field.getContainingClass();
    if (!GormUtils.isGormBean(domainClass)) return;

    @SuppressWarnings("ConstantConditions")
    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(domainClass);

    if (argumentName == null) {
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : descriptor.getPersistentProperties().entrySet()) {
        result.put(entry.getKey(), new StringTypeCondition(CommonClassNames.JAVA_LANG_STRING, entry.getValue().second));
      }
    }
    else {
      Pair<PsiType, PsiElement> p = descriptor.getPersistentProperties().get(argumentName);
      if (p != null) {
        result.put(argumentName, new StringTypeCondition(CommonClassNames.JAVA_LANG_STRING, p.second));
      }
    }
  }
}

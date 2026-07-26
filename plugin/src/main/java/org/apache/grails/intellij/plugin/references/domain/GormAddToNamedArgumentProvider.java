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

package org.apache.grails.intellij.plugin.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.psi.util.PsiUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.extensions.impl.TypeCondition;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;

import java.util.Map;

public class GormAddToNamedArgumentProvider extends GroovyNamedArgumentProvider {
  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    PsiElement resolve = resolveResult.getElement();
    if (!(resolve instanceof PsiMethod method)) return;

    String methodName = method.getName();
    assert methodName.startsWith("addTo");

    String propertyName = StringUtil.decapitalize(methodName.substring("addTo".length()));

    PsiType domainClassType = method.getReturnType();
    assert domainClassType != null;
    PsiClass domainClass = ((PsiClassType)domainClassType).resolve();

    assert GormUtils.isGormBean(domainClass);
    assert domainClass != null;

    Pair<PsiType,PsiElement> pair = DomainDescriptor.getPersistentProperties(domainClass).get(propertyName);
    if (pair == null) return;

    PsiClass referencedDomain = PsiTypesUtil.getPsiClass(PsiUtil.extractIterableTypeParameter(pair.first, true));
    if (!GormUtils.isGormBean(referencedDomain)) return;
    assert referencedDomain != null;

    DomainDescriptor referencedDescriptor = DomainDescriptor.getDescriptor(referencedDomain);

    if (argumentName == null) {
      for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : referencedDescriptor.getPersistentProperties().entrySet()) {
        PsiType fieldType = entry.getValue().first;
        result.put(entry.getKey(), new TypeCondition(fieldType, entry.getValue().second));
      }
    }
    else {
      Pair<PsiType, PsiElement> p = referencedDescriptor.getPersistentProperties().get(argumentName);
      if (p != null) {
        result.put(argumentName, new TypeCondition(p.first, p.second));
      }
    }
  }
}

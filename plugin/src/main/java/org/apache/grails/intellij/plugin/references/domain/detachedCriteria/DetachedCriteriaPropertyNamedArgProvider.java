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

package org.apache.grails.intellij.plugin.references.domain.detachedCriteria;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.domain.GormPersistentPropertiesNamedArgProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

public class DetachedCriteriaPropertyNamedArgProvider extends GormPersistentPropertiesNamedArgProvider {

  @Override
  protected PsiClass getDomainClass(@NotNull GrMethodCall call, PsiMethod resolve, GroovyResolveResult resolveResult) {
    if (GrLightMethodBuilder.checkKind(resolve, DetachedCriteriaClosureMemberProvider.MARKER)) {
      return ((GrLightMethodBuilder)resolve).getData();
    }

    return DetachedCriteriaUtil.getDomainFromSubstitutor(resolveResult);
  }
}

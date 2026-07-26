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

package org.apache.grails.intellij.plugin.pluginSupport.webflow;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

import java.util.Map;

public class WebFlowStateNameReferenceProvider extends GrailsMethodNamedArgumentReferenceProvider.Contributor.Provider {
  @Override
  public PsiReference[] createRef(@NotNull PsiElement element,
                                  @NotNull GrMethodCall m,
                                  int argumentIndex,
                                  @NotNull GroovyResolveResult resolveResult) {
    final GrClosableBlock stateDefClosure = PsiTreeUtil.getParentOfType(element, GrClosableBlock.class);
    if (stateDefClosure == null) return PsiReference.EMPTY_ARRAY;

    PsiElement parent = stateDefClosure.getParent();
    if (parent instanceof GrArgumentList) parent = parent.getParent();

    if (!(parent instanceof GrMethodCall methodCall)) return PsiReference.EMPTY_ARRAY;

    if (!WebFlowUtils.isStateDeclaration(methodCall, true)) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[] {
      new PsiReferenceBase<>(element, false) {

        private Map<String, PsiVariable> getStates() {
          GrField actionDedField = WebFlowUtils.getActionByStateDeclaration(methodCall);
          return WebFlowUtils.getWebFlowStates(actionDedField);
        }

        @Override
        public PsiElement resolve() {
          return getStates().get(getValue());
        }

        @Override
        public Object @NotNull [] getVariants() {
          return getStates().keySet().toArray();
        }
      }
    };
  }
}

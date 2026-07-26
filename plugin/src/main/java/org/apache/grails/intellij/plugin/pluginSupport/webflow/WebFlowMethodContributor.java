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

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class WebFlowMethodContributor extends ClosureMemberContributor {
  @Override
  protected void processMembers(@NotNull GrClosableBlock closure, @NotNull PsiScopeProcessor processor, @NotNull PsiElement place, @NotNull ResolveState state) {
    PsiElement parent = closure.getParent();
    if (!(parent instanceof GrField)) return;

    if (!(WebFlowUtils.isFlowActionField((GrField)parent))) return;

    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return;

    String nameHint = ResolveUtil.getNameHint(processor);

    // Process variable 'flow'
    if (nameHint == null || "getFlow".equals(nameHint)) {
      GrLightMethodBuilder cachedGetFlow = CachedValuesManager.getCachedValue(parent, () -> {
        GrLightMethodBuilder method = new GrLightMethodBuilder(parent.getManager(), "getFlow");
        method.setReturnType(CommonClassNames.JAVA_UTIL_MAP, parent.getResolveScope());
        method.setData(((GrField)parent).getName());
        return CachedValueProvider.Result.create(method, PsiModificationTracker.MODIFICATION_COUNT);
      });
      if (!processor.execute(cachedGetFlow, state) || nameHint != null) return;
    }

    // Process variable 'conversation'
    if (nameHint == null || "getConversation".equals(nameHint)) {
      GrLightMethodBuilder cachedGetFlow = CachedValuesManager.getCachedValue(parent, () -> {
        GrLightMethodBuilder method = new GrLightMethodBuilder(parent.getManager(), "getConversation");
        method.setReturnType(CommonClassNames.JAVA_UTIL_MAP, parent.getResolveScope());
        method.setData(((GrField)parent).getName());
        return CachedValueProvider.Result.create(method, PsiModificationTracker.MODIFICATION_COUNT);
      });

      if (!processor.execute(cachedGetFlow, state) || nameHint != null) return;
    }

    // Process methods from FlowInfoCapturer.
    PsiClass flowInfoClass = JavaPsiFacade.getInstance(closure.getProject())
      .findClass("org.codehaus.groovy.grails.webflow.engine.builder.FlowInfoCapturer", closure.getResolveScope());

    if (flowInfoClass != null) {
      flowInfoClass.processDeclarations(processor, state, null, place);
    }
  }

}

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

package org.jetbrains.plugins.grails.pluginSupport.resources;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class GrailsResourcesClosureMethodContributor extends ClosureMemberContributor {
  @Override
  protected void processMembers(@NotNull GrClosableBlock closure, @NotNull PsiScopeProcessor processor, @NotNull PsiElement place, @NotNull ResolveState state) {
    PsiElement parent = closure.getParent();
    if (parent instanceof GrArgumentList) parent = parent.getParent();

    if (!(parent instanceof GrMethodCall)) return;

    if (!GrailsResourcesUtil.isModuleDefinition((GrMethodCall)parent)) return;

    if (!ResolveUtil.shouldProcessMethods(processor.getHint(ElementClassHint.KEY))) return;

    GrailsPsiUtil.process(GrailsResourcesUtil.MODULE_BUILDER_CLASS, processor, place, state);
  }
}

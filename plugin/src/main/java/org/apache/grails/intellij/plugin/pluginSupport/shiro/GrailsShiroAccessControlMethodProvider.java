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

package org.apache.grails.intellij.plugin.pluginSupport.shiro;

import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.GrailsClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;

public class GrailsShiroAccessControlMethodProvider implements GrailsClosureMemberContributor.MethodProvider {

  @Override
  public boolean processMembers(@NotNull GrClosableBlock closure,
                                PsiClass artifactClass,
                                PsiScopeProcessor processor,
                                GrReferenceExpression refExpr,
                                ResolveState state) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(artifactClass.getProject());
    GlobalSearchScope resolveScope = refExpr.getResolveScope();

    PsiClass builderClass = facade.findClass("org.apache.shiro.grails.AccessControlBuilder", resolveScope);
    if (builderClass == null) {
      builderClass = facade.findClass("org.jsecurity.grails.AccessControlBuilder", resolveScope);

      if (builderClass == null) return true;
    }

    return builderClass.processDeclarations(processor, state, null, refExpr);
  }
}

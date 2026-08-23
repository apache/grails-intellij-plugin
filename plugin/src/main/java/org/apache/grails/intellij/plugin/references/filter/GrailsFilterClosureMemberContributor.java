/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.grails.intellij.plugin.references.filter;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiVariable;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrImplicitVariableImpl;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class GrailsFilterClosureMemberContributor extends ClosureMemberContributor {
  private static final String[] PROPERTIES = {"before", "after", "afterView"};

  @Override
  public void processMembers(@NotNull GrClosableBlock closure,
                             @NotNull PsiScopeProcessor processor,
                             @NotNull PsiElement place,
                             @NotNull ResolveState state) {
    if (!(place instanceof GrReferenceExpression refExpr)) return;
    if (refExpr.isQualified()) return;

    if (ResolveUtil.shouldProcessProperties(processor.getHint(ElementClassHint.KEY))) {
      String name = ResolveUtil.getNameHint(processor);
      if (name != null && !ArrayUtil.contains(name, PROPERTIES)) return;

      PsiElement eMethodCall = closure.getParent();

      if (!GrailsFilterUtil.isFilterDefinitionMethod(eMethodCall)) return;

      PsiManager manager = eMethodCall.getManager();

      if (name == null) {
        for (String property : PROPERTIES) {
          PsiVariable var = new GrImplicitVariableImpl(manager, property, CommonClassNames.JAVA_LANG_OBJECT, refExpr);
          if (!processor.execute(var, state)) return;
        }
      }
      else {
        PsiVariable var = new GrImplicitVariableImpl(manager, name, CommonClassNames.JAVA_LANG_OBJECT, refExpr);
        if (!processor.execute(var, state)) //noinspection UnnecessaryReturnStatement
          return;
      }
    }
  }

}

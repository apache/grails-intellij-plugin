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

package org.jetbrains.plugins.grails.references.urlMappings;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.plugins.grails.references.constraints.GrailsConstraintsUtil;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.Map;

final class UrlMappingConstraintClosureMemberProvider extends ClosureMissingMethodContributor {
  private UrlMappingConstraintClosureMemberProvider() {
  }

  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state) {
    PsiElement eConstraintsMethodCall = closure.getParent();
    if (eConstraintsMethodCall instanceof GrArgumentList) eConstraintsMethodCall = eConstraintsMethodCall.getParent();
    if (!(eConstraintsMethodCall instanceof GrMethodCall constraintsMethodCall)) return true;

    if (!PsiUtil.isReferenceWithoutQualifier(constraintsMethodCall.getInvokedExpression(), "constraints")) return true;
    if (PsiUtil.getAllArguments(constraintsMethodCall).length != 1) return true;

    PsiElement eCloseableBlock = constraintsMethodCall.getParent();
    if (!(eCloseableBlock instanceof GrClosableBlock)) return true;

    PsiElement outMethodCall = eCloseableBlock.getParent();
    if (outMethodCall instanceof GrArgumentList) outMethodCall = outMethodCall.getParent();
    if (!(outMethodCall instanceof GrMethodCall)) return true;

    if (!UrlMappingUtil.isMappingDefinition((GrMethodCall)outMethodCall)) return true;

    Map<String,Pair<PsiElement, Boolean>> map = UrlMappingUtil.getParamsByInvokedExpression(((GrMethodCall)outMethodCall).getInvokedExpression());
    if (map == null || map.isEmpty()) return true;

    String nameHint = ResolveUtil.getNameHint(processor);

    if (nameHint == null) {
      for (Map.Entry<String, Pair<PsiElement, Boolean>> entry : map.entrySet()) {
        PsiMethod method = GrailsConstraintsUtil.createMethod(entry.getKey(), entry.getValue().first,
                                                              TypesUtil.createTypeByFQClassName(
                                                                CommonClassNames.JAVA_LANG_STRING,
                                                                outMethodCall),
                                                              null);
        if (!processor.execute(method, state)) return false;
      }
    }
    else {
      Pair<PsiElement, Boolean> pair = map.get(nameHint);
      if (pair != null) {
        PsiMethod method = GrailsConstraintsUtil.createMethod(nameHint, pair.first,
                                                              TypesUtil.createTypeByFQClassName(
                                                                CommonClassNames.JAVA_LANG_STRING,
                                                                outMethodCall),
                                                              null);
        if (!processor.execute(method, state)) return false;
      }
    }

    if (!GrailsConstraintsUtil.processImportFromMethod(processor, state, outMethodCall, nameHint)) return false;

    return true;
  }

}

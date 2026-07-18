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

package org.jetbrains.plugins.grails.references.controller;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiTypes;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

/**
 * Resolves method inside closure passed to ControllerApi.withFormat method.
 *
 * class CccController {
 *   def index = {
 *     withFormat {
 *       html {}
 *     }
 *   }
 * }
 */
public class GrailsWithFormatClosureMethodContributor extends ClosureMissingMethodContributor {
  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state) {
    String name = refExpr.getReferenceName();
    String nameHint = ResolveUtil.getNameHint(processor);
    if (nameHint == null || !nameHint.equals(name)) return true;

    if (refExpr.isQualified()) return true;
    PsiElement formatMethodCall = refExpr.getParent();
    if (!(formatMethodCall instanceof GrMethodCall)) return true;
    if (formatMethodCall.getParent() != closure) return true;
    GrExpression[] allArguments = PsiUtil.getAllArguments((GrMethodCall)formatMethodCall);
    if (allArguments.length != 1) return true;

    GrExpression arg = allArguments[0];

    // See org.codehaus.groovy.grails.plugins.web.mimes.FormatInterceptor
    if (!(arg instanceof GrClosableBlock)
        && !(arg instanceof GrListOrMap) && ((GrMethodCall)formatMethodCall).getNamedArguments().length == 0) return true;

    GrLightMethodBuilder builder = new GrLightMethodBuilder(closure.getManager(), name);

    if (arg instanceof GrClosableBlock) {
      builder.addParameter("closure", GroovyCommonClassNames.GROOVY_LANG_CLOSURE);
    }
    else {
      builder.addParameter("map", CommonClassNames.JAVA_UTIL_MAP);
    }

    builder.setReturnType(PsiTypes.voidType());

    if (!processor.execute(builder, state)) return false;

    return true;
  }
}

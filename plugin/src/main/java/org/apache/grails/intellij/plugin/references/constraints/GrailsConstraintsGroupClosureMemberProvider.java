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

package org.apache.grails.intellij.plugin.references.constraints;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class GrailsConstraintsGroupClosureMemberProvider extends ClosureMissingMethodContributor {
  private static final Object CONSTRAINT_GROUP_METHOD_MARKER = "Grails:ConstraintGroup:method";

  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state) {
    PsiElement methodCall = refExpr.getParent();
    if (methodCall == null) return true;
    if (methodCall.getParent() != closure) return true;

    String nameHint = ResolveUtil.getNameHint(processor);
    if (nameHint == null) return true;

    PsiElement eAssignment = closure.getParent();
    if (!(eAssignment instanceof GrAssignmentExpression)) return true;

    PsiElement eFile = eAssignment.getParent();
    if (!(eFile instanceof GroovyFile)) return true;

    GrExpression lValue = ((GrAssignmentExpression)eAssignment).getLValue();
    if (!(lValue instanceof GrReferenceExpression)) return true;
    if (!lValue.getText().equals(GrailsConstraintsUtil.GRAILS_GORM_DEFAULT_CONSTRAINTS)) return true;

    if (!GrailsUtils.isConfigGroovyFile(eFile)) return true;

    if (refExpr.isQualified()) return true;

    GrLightMethodBuilder res = new GrLightMethodBuilder(eFile.getManager(), nameHint);
    res.addOptionalParameter("constraints", CommonClassNames.JAVA_UTIL_MAP);
    res.setMethodKind(CONSTRAINT_GROUP_METHOD_MARKER);

    return processor.execute(res, state);
  }
}

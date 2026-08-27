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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

final class GrailsConstraintsClosureMemberProvider extends ClosureMissingMethodContributor {
  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state) {
    PsiElement eField = closure.getParent();
    if (!(eField instanceof GrField field)) return true;

    String fieldName = field.getName();

    if (!"constraints".equals(fieldName) || !field.hasModifierProperty(PsiModifier.STATIC)) return true;

    PsiClass aClass = field.getContainingClass();
    if (aClass == null || !GrailsUtils.isValidatedClass(aClass)) return true;

    String nameHint = ResolveUtil.getNameHint(processor);

    if (nameHint == null) {
      for (PsiField psiField : aClass.getAllFields()) {
        if (psiField.hasModifierProperty(PsiModifier.STATIC)) continue;

        PsiMethod method = GrailsConstraintsUtil.createMethod(psiField.getName(), psiField, psiField.getType(), aClass);
        if (!processor.execute(method, ResolveState.initial())) return false;
      }
    }
    else {
      PsiField psiField = aClass.findFieldByName(nameHint, true);
      if (psiField != null && !psiField.hasModifierProperty(PsiModifier.STATIC)) {
        PsiMethod method = GrailsConstraintsUtil.createMethod(nameHint, psiField, psiField.getType(), aClass);
        if (!processor.execute(method, ResolveState.initial())) return false;
      }
    }

    if (!GrailsConstraintsUtil.processImportFromMethod(processor, state, aClass, nameHint)) return false;

    return true;
  }
}

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

package org.apache.grails.intellij.plugin.config;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.apache.grails.intellij.plugin.util.GrailsPsiUtil;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrAssignmentExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;

final class BuildConfigClosureMemberContributor extends ClosureMissingMethodContributor {
  private static boolean isDependencyResolutionClosure(GrClosableBlock closure) {
    PsiElement parent = closure.getParent();
    if (!(parent instanceof GrAssignmentExpression)) return false;

    if (((GrAssignmentExpression)parent).isOperatorAssignment()) return false;

    GrExpression lValue = ((GrAssignmentExpression)parent).getLValue();
    if (!(lValue instanceof GrReferenceExpression) || !lValue.getText().equals("grails.project.dependency.resolution")) return false;

    PsiFile containingFile = parent.getContainingFile();
    return containingFile != null && GrailsUtils.BUILD_CONFIG.equals(containingFile.getName());
  }

  private static boolean isRepositoriesClosure(GrClosableBlock closure) {
    PsiElement parent = closure.getParent();

    if (!(parent instanceof GrMethodCall)) return false;

    if (!PsiUtil.isMethodCall((GrMethodCall)parent, "repositories")) return false;

    PsiElement closure2 = parent.getParent();

    return closure2 instanceof GrClosableBlock && isDependencyResolutionClosure((GrClosableBlock)closure2);
  }

  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state) {
    if (isDependencyResolutionClosure(closure)) {
      GrailsStructure structure = GrailsStructure.getInstance(closure);
      if (structure == null) return true;

      String delegateClassName = structure.isAtLeastGrails1_4()
                                 ? "org.codehaus.groovy.grails.resolve.config.DependencyConfigurationConfigurer"
                                 : "org.codehaus.groovy.grails.resolve.IvyDomainSpecificLanguageEvaluator";

      return GrailsPsiUtil.process(delegateClassName, processor, refExpr, state);
    }

    if (isRepositoriesClosure(closure)) {
      GrailsStructure structure = GrailsStructure.getInstance(closure);
      if (structure == null) return true;

      String configurerName;

      if (structure.isAtLeastGrails("2.3.0")) {
        configurerName = "org.codehaus.groovy.grails.resolve.maven.aether.config.RepositoriesConfiguration";
      }
      else {
        configurerName = "org.codehaus.groovy.grails.resolve.config.RepositoriesConfigurer";
      }

      return GrailsPsiUtil.process(configurerName, processor, refExpr, state);
    }

    return true;
  }

}

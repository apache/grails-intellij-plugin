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

package org.apache.grails.intellij.plugin.references.jobs;

import com.intellij.psi.PsiClass;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.GrailsClosureMemberContributor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

public final class JobClosureMethodProvider implements GrailsClosureMemberContributor.MethodProvider {

  private static final String CLASS_SOURCE = """
    class JobTriggerElements {  private void simple(Map arg) { def z = arg.name + arg.startDelay + arg.repeatInterval + arg.repeatCount}
      private void cron(Map arg) { def z = arg.name + arg.startDelay + arg.cronExpression}
      private void custom(Map arg) { def z = arg.triggerClass}
    }""";

  @Override
  public boolean processMembers(@NotNull GrClosableBlock closure,
                                PsiClass artifactClass,
                                PsiScopeProcessor processor,
                                GrReferenceExpression refExpr,
                                ResolveState state) {
    return DynamicMemberUtils.process(processor, false, refExpr, CLASS_SOURCE);
  }
}

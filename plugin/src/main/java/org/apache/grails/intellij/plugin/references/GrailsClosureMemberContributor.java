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

package org.apache.grails.intellij.plugin.references;

import com.intellij.openapi.util.Trinity;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.pluginSupport.shiro.GrailsShiroAccessControlMethodProvider;
import org.apache.grails.intellij.plugin.references.domain.GormConstraintMethodProvider;
import org.apache.grails.intellij.plugin.references.domain.GormMappingMethodProvider;
import org.apache.grails.intellij.plugin.references.domain.GormUtils;
import org.apache.grails.intellij.plugin.references.filter.FilterClosureMemberProvider;
import org.apache.grails.intellij.plugin.references.jobs.JobClosureMethodProvider;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.resolve.ClosureMissingMethodContributor;

import java.util.HashMap;
import java.util.Map;

public final class GrailsClosureMemberContributor extends ClosureMissingMethodContributor {
  private static final Map<String, Trinity<GrailsArtifact, Boolean, ? extends MethodProvider>> map = new HashMap<>();
  static {
    map.put("triggers", Trinity.create(GrailsArtifact.JOB, true, new JobClosureMethodProvider()));
    map.put("filters", Trinity.create(GrailsArtifact.FILTER, false, new FilterClosureMemberProvider()));

    map.put("constraints", Trinity.create(GrailsArtifact.DOMAIN, true, new GormConstraintMethodProvider()));
    map.put("mapping", Trinity.create(GrailsArtifact.DOMAIN, true, new GormMappingMethodProvider()));
    map.put("accessControl", Trinity.create(GrailsArtifact.CONTROLLER, true, new GrailsShiroAccessControlMethodProvider()));
  }

  @Override
  public boolean processMembers(GrClosableBlock closure, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state) {
    PsiElement eField = closure.getParent();
    if (!(eField instanceof GrField field)) return true;

    String fieldName = field.getName();

    Trinity<GrailsArtifact, Boolean, ? extends MethodProvider> trinity = map.get(fieldName);
    if (trinity == null) return true;

    PsiClass aClass = field.getContainingClass();
    if (!trinity.first.isInstance(aClass) && !(trinity.first == GrailsArtifact.DOMAIN && GormUtils.isStandaloneGormBean(aClass))) {
      return true;
    }

    if (trinity.second && !field.hasModifierProperty(PsiModifier.STATIC)) return true;

    if (!trinity.third.processMembers(closure, aClass, processor, refExpr, state)) return false;

    return true;
  }

  public interface MethodProvider {
    boolean processMembers(@NotNull GrClosableBlock closure, PsiClass artifactClass, PsiScopeProcessor processor, GrReferenceExpression refExpr, ResolveState state);
  }
}

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

package org.apache.grails.intellij.plugin.references.domain.detachedCriteria;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiSubstitutor;
import com.intellij.psi.PsiType;
import com.intellij.psi.PsiTypeParameter;
import com.intellij.psi.impl.source.PsiImmediateClassType;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.references.domain.DomainDescriptor;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

import java.util.Map;

public final class DetachedCriteriaUtil {

  public static final String DETACHED_CRITERIA_CLASS = "grails.gorm.DetachedCriteria";

  private DetachedCriteriaUtil() {
  }

  public static boolean isDomainDetachedCriteriaMethod(@NotNull PsiMethod method) {
    if (GrLightMethodBuilder.checkKind(method, DomainDescriptor.DOMAIN_DYNAMIC_METHOD)) {
      String methodName = method.getName();
      return "where".equals(methodName) || "findAll".equals(methodName) || "find".equals(methodName);
    }

    return false;
  }

  public static @Nullable PsiClass getDomainFromSubstitutor(@NotNull GroovyResolveResult resolveResult) {
    PsiSubstitutor substitutor = resolveResult.getSubstitutor();
    Map<PsiTypeParameter,PsiType> substitutionMap = substitutor.getSubstitutionMap();
    if (substitutionMap.size() != 1) return null;

    PsiClass res = PsiTypesUtil.getPsiClass(substitutionMap.values().iterator().next());
    return GrailsArtifact.DOMAIN.isInstance(res) ? res : null;
  }

  public static @Nullable PsiClass getDomainClassByDetachedCriteriaExpression(@Nullable PsiType type) {
    if (!(type instanceof PsiImmediateClassType)) return null;

    PsiClass detachedCriteriaClass = PsiTypesUtil.getPsiClass(type);
    if (detachedCriteriaClass == null || !DETACHED_CRITERIA_CLASS.equals(detachedCriteriaClass.getQualifiedName())) {
      return null;
    }

    PsiType[] parameters = ((PsiImmediateClassType)type).getParameters();
    if (parameters.length != 1) return null;

    PsiClass domainClass = PsiTypesUtil.getPsiClass(parameters[0]);
    if (!GrailsArtifact.DOMAIN.isInstance(domainClass)) return null;

    return domainClass;
  }
}

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

package org.apache.grails.intellij.plugin.references.controller;

import com.intellij.javaee.web.WebCommonClassNames;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassType;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTypesUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.config.GrailsStructure;
import org.apache.grails.intellij.plugin.util.GrailsPsiUtil;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;

final class HttpResponseMemberContributor extends NonCodeMembersContributor {
  @Override
  protected @Nullable String getParentClassName() {
    return WebCommonClassNames.JAVAX_HTTP_SERVLET_RESPONSE;
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (aClass == null) return;

    GrailsStructure grailsStructure = GrailsStructure.getInstance(place);
    if (grailsStructure == null) return;

    Project project = aClass.getProject();

    GlobalSearchScope resolveScope = place.getResolveScope();

    PsiClassType httpResponseType = PsiTypesUtil.getClassType(aClass);

    PsiClass responceMimeApi = JavaPsiFacade.getInstance(project).findClass("org.codehaus.groovy.grails.plugins.web.api.ResponseMimeTypesApi", resolveScope);
    if (responceMimeApi != null) {
      if (!GrailsPsiUtil.enhance(processor, responceMimeApi, httpResponseType)) return;
    }
  }
}

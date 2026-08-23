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

package org.apache.grails.intellij.plugin.references.domain;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

public class GormNamedArgumentReferenceProvider extends GrailsMethodNamedArgumentReferenceProvider.Contributor.Provider implements GrailsMethodNamedArgumentReferenceProvider.Contributor {
  @Override
  public void register(GrailsMethodNamedArgumentReferenceProvider registrar) {
    registrar.register("sort", this, new LightMethodCondition(DomainDescriptor.DOMAIN_DYNAMIC_METHOD), "list");
    registrar.register(0, this, new LightMethodCondition(DomainDescriptor.DOMAIN_DYNAMIC_METHOD), "isDirty");
  }

  @Override
  protected PsiReference[] createRef(@NotNull PsiElement element, @NotNull GroovyResolveResult resolveResult) {
    PsiClass domainClass = ((GrLightMethodBuilder)resolveResult.getElement()).getData();
    return new PsiReference[]{new GormPropertyReference(element, false, domainClass)};
  }

}

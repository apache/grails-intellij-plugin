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

package org.apache.grails.intellij.plugin.references.domain.namedQuery;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMethod;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.domain.GormPersistentPropertiesNamedArgProvider;
import org.apache.grails.intellij.plugin.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

public class NamedCriteriaProxyNamedArgumentProvider extends GormPersistentPropertiesNamedArgProvider {

  @Override
  protected PsiClass getDomainClass(@NotNull GrMethodCall call, PsiMethod resolve, GroovyResolveResult resolveResult) {
    NamedQueryDescriptor queryDescriptor = GormUtils.getQueryDescriptorByProxyMethod(call);
    if (queryDescriptor == null) return null;

    return queryDescriptor.getDomainClass();
  }
}

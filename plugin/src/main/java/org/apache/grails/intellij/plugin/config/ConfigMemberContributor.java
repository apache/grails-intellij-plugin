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

package org.apache.grails.intellij.plugin.config;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

final class ConfigMemberContributor extends NonCodeMembersContributor {
  private static final String CLASS_SOURCE = "class BuildConfigMembers {" +
                                             "  public String getGrailsHome(){}" +
                                             "  public String getAppName() {}" +
                                             "  public String getAppVersion() {}" +
                                             "  public String getBasedir() {}" +
                                             "  public java.io.File getUserHome() {}" +
                                             "  public String getServletVersion() {}" +
                                             "}";

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!GrailsUtils.isConfigGroovyFile(place.getContainingFile().getOriginalFile())) return;
    if (!"Config".equals(TypesUtil.getQualifiedName(qualifierType))) return;

    DynamicMemberUtils.process(processor, false, place, CLASS_SOURCE);
  }
}

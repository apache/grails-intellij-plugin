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

package org.jetbrains.plugins.grails.config;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

final class BuildConfigMemberContributor extends NonCodeMembersContributor {
  // #CHECK# see BuildSettings.createConfigSlurper()
  private static final String CLASS_SOURCE = "class BuildConfigMembers {" +
                                             "  public String getBasedir() {}" +
                                             "  public java.io.File getBaseFile() {}" +
                                             "  public String getBaseName() {}" +
                                             "  public String getGrailsHome(){}" +
                                             "  public String getGrailsVersion() {}" +
                                             "  public java.io.File getUserHome() {}" +
                                             "  public grails.util.BuildSettings getGrailsSettings() {}" +
                                             "  public String getAppName() {}" +
                                             "  public String getAppVersion() {}" +
                                             "}";

  @Override
  protected String getParentClassName() {
    return "BuildConfig";
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!GrailsUtils.isBuildConfigFile(place.getContainingFile())) return;

    DynamicMemberUtils.process(processor, false, place, CLASS_SOURCE);
  }

}

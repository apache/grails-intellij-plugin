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

package org.jetbrains.plugins.grails.references;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.util.dynamicMembers.DynamicMemberUtils;

final class GrailsResourceClassMemberContributor extends NonCodeMembersContributor {
  private static final String CODE = """
    class GrailsPluginMembers {  private final org.codehaus.groovy.grails.commons.GrailsApplication application;
      private final org.codehaus.groovy.grails.commons.GrailsApplication grailsApplication;
    }\s""";

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (aClass == null) return;

    String className = aClass.getName();

    if (className == null || !className.endsWith("resources")) return;

    String qname = aClass.getQualifiedName();
    if (qname == null || qname.contains(".")) return;

    if (ResolveUtil.shouldProcessProperties(processor.getHint(ElementClassHint.KEY))) {
      String name = ResolveUtil.getNameHint(processor);

      for (PsiField field : DynamicMemberUtils.getMembers(place.getProject(), CODE).getFields(name)) {
        if (!processor.execute(field, ResolveState.initial())) return;
      }
    }
  }

}

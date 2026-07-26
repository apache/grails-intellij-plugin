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

package org.apache.grails.intellij.plugin.lang.gsp;

import com.intellij.codeInspection.LocalInspectionTool;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.xml.XmlTagRuleProvider;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.impl.gtag.GspTagRuleProvider;

public final class GspTagInspection extends LocalInspectionTool {

  private final GspTagRuleProvider ruleProvider = new GspTagRuleProvider();

  @Override
  public @NotNull PsiElementVisitor buildVisitor(final @NotNull ProblemsHolder holder, final boolean isOnTheFly) {
    return new PsiElementVisitor() {
      @Override
      public void visitElement(@NotNull PsiElement element) {
        if (element instanceof GspGrailsTag tag) {
          for (XmlTagRuleProvider.Rule rule : ruleProvider.getTagRule(tag)) {
            rule.annotate(tag, holder);
          }
        }
      }
    };
  }
}

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

import com.intellij.codeInsight.highlighting.HighlightErrorFilter;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiErrorElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.PsiImplUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspExprInjection;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GspGroovyFile;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GspOuterGroovyElement;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspTag;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.gtag.GspAttribute;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.gtag.GspAttributeValue;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrConditionalExpression;

public final class GspErrorFilter extends HighlightErrorFilter {
  @Override
  public boolean shouldHighlightErrorElement(@NotNull PsiErrorElement element) {
    PsiElement parent = element.getParent();
    if (!(parent instanceof GrConditionalExpression)) return true;
    if (element.getNextSibling() != null) return true;
    if (!PsiImplUtil.isLeafElementOfType(element.getPrevSibling(), GroovyTokenTypes.mQUESTION)) return true;
    if (element.getTextLength() != 0) return true;

    PsiElement exprInj = parent.getParent();
    if (!(exprInj instanceof GrGspExprInjection)) return true;

    PsiFile containingFile = exprInj.getContainingFile();
    if (!(containingFile instanceof GspGroovyFile)) return true;

    PsiElement xmlTagElement = containingFile.getViewProvider().findElementAt(exprInj.getTextOffset(), GspLanguage.INSTANCE);
    if (!(xmlTagElement instanceof GspOuterGroovyElement)) return true;
    PsiElement attributeValue = xmlTagElement.getParent();
    if (!(attributeValue instanceof GspAttributeValue)) return true;

    PsiElement gspAttribute = attributeValue.getParent();
    if (!(gspAttribute instanceof GspAttribute)) return true;

    if (!"in".equals(((GspAttribute)gspAttribute).getName())) return true;

    PsiElement tag = gspAttribute.getParent();
    if (!(tag instanceof GspTag)) return true;

    String tagName = ((GspTag)tag).getName();
    if (!"g:each".equals(tagName)
        && !"g:grep".equals(tagName)
        && !"g:collect".equals(tagName)
        && !"g:findAll".equals(tagName)) {
      return true;
    }

    return false;
  }
}

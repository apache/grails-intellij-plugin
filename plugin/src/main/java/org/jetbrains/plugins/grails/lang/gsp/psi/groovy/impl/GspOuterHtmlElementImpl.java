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

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.lang.ASTNode;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.LiteralTextEscaper;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.impl.source.tree.LeafPsiElement;
import com.intellij.psi.templateLanguages.OuterLanguageElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.psi.GspPsiElementFactory;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterHtmlElement;

public class GspOuterHtmlElementImpl extends LeafPsiElement implements OuterLanguageElement, GspOuterHtmlElement {

  public GspOuterHtmlElementImpl(@NotNull IElementType type, CharSequence text) {
    super(type, text);
  }

  @Override
  public String toString() {
    return "Outer: " + getElementType();
  }

  @Override
  public boolean isValidHost() {
    return true;
  }

  @Override
  public PsiLanguageInjectionHost updateText(final @NotNull String text) {
    ASTNode node = getNode();
    if (node == null) return this;
    ASTNode parent = node.getTreeParent();
    GspPsiElementFactory factory = GspPsiElementFactory.getInstance(getProject());
    GspOuterHtmlElement outer = factory.createOuterHtmlElement(text);
    ASTNode outerNode = outer.getNode();
    assert outerNode != null;
    parent.replaceChild(node, outerNode);

    return outer;
  }

  /**
   * @return escapre for other language occurrences in string literal
   */
  @Override
  public @NotNull LiteralTextEscaper<GspOuterHtmlElement> createLiteralTextEscaper() {
    return new LiteralTextEscaper<>(this) {

      @Override
      public boolean decode(@NotNull TextRange rangeInsideHost, @NotNull StringBuilder outChars) {
        outChars.append(getChars(), rangeInsideHost.getStartOffset(), rangeInsideHost.getEndOffset());
        return true;
      }

      /**
       * Returns offset in host (start or end)
       *
       */
      @Override
      public int getOffsetInHost(int offsetInDecoded, @NotNull TextRange rangeInsideHost) {
        if (offsetInDecoded <= rangeInsideHost.getEndOffset()) {
          return offsetInDecoded;
        }

        return -1;
      }

      @Override
      public boolean isOneLine() {
        return false;
      }
    };
  }

}

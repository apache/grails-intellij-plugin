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
package org.apache.grails.intellij.plugin.lang.gsp.highlighter;

import com.intellij.javaee.el.impl.ELHighlighter;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.JspHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.GspFlexLexer;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.groovy.highlighter.GroovySyntaxHighlighter;

public final class GspSyntaxHighlighter extends SyntaxHighlighterBase implements GspTokenTypesEx {
  private final GspDirectiveHighlighter myDirectiveHighlighter = new GspDirectiveHighlighter();

  @Override
  public @NotNull Lexer getHighlightingLexer() {
    return new GspFlexLexer();
  }

  static final TokenSet tGSP_SEPARATORS_NOT_DIRECT = TokenSet.create(
          JSCRIPT_BEGIN,
          JDECLAR_BEGIN,
          JDECLAR_END,
          JEXPR_BEGIN,
          JSCRIPT_END,
          JEXPR_END,
          GEXPR_BEGIN,
          GEXPR_END,
          GSTRING_DOLLAR,
          GSCRIPT_BEGIN,
          GSCRIPT_END,
          GDECLAR_BEGIN,
          GDECLAR_END);

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
    if (tGSP_SEPARATORS_NOT_DIRECT.contains(tokenType)) {
      return pack(ELHighlighter.EL_BOUNDS);
    }
    if (GspTokenTypesEx.GSP_COMMENTS.contains(tokenType)) {
      return pack(GroovySyntaxHighlighter.BLOCK_COMMENT);
    }
    if (myDirectiveHighlighter.getTokenHighlights(tokenType).length > 0) {
      return pack(JspHighlighterColors.JSP_ACTION_AND_DIRECTIVE_BACKGROUND, myDirectiveHighlighter.getTokenHighlights(tokenType));
    }
    return TextAttributesKey.EMPTY_ARRAY;
  }
}

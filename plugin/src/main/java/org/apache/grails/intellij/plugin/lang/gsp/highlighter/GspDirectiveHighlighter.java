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
package org.apache.grails.intellij.plugin.lang.gsp.highlighter;

import com.intellij.lexer.Lexer;
import com.intellij.openapi.editor.JspHighlighterColors;
import com.intellij.openapi.editor.colors.TextAttributesKey;
import com.intellij.openapi.fileTypes.SyntaxHighlighterBase;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes;

public class GspDirectiveHighlighter extends SyntaxHighlighterBase implements GspTokenTypes {
  static final TokenSet tGSP_DIRECT_SEPARATORS = TokenSet.create(
      GTAG_END_TAG_START,
      GTAG_START_TAG_START,
      GTAG_START_TAG_END,
      GTAG_TAG_END,
      JDIRECT_BEGIN,
      JDIRECT_END,
      GDIRECT_BEGIN,
      GDIRECT_END);

  static final TokenSet tGSP_DIRECT_TOKENS = TokenSet.create(
      GSP_WHITE_SPACE,
      GSP_TAG_NAME,
      GSP_ATTR_NAME,
      GSP_EQ,
      GSP_BAD_CHARACTER,
      GSP_ATTR_VALUE_START_DELIMITER,
      GSP_ATTR_VALUE_END_DELIMITER,
      GSP_ATTRIBUTE_VALUE_TOKEN
  );

  @Override
  public @NotNull Lexer getHighlightingLexer() {
    return new GspDirectiveHighlightingLexer();
  }

  @Override
  public TextAttributesKey @NotNull [] getTokenHighlights(IElementType tokenType) {
    if (tGSP_DIRECT_SEPARATORS.contains(tokenType)) {
      return pack(JspHighlighterColors.JSP_DIRECTIVE_STAT_END_MARKER);
    }
    if (tokenType == GSP_ATTR_NAME) {
      return pack(JspHighlighterColors.JSP_ATTRIBUTE_NAME);
    }
    if (tokenType == GSP_TAG_NAME) {
      return pack(JspHighlighterColors.JSP_ACTION_AND_DIRECTIVE_NAME);
    }
    if (tokenType == GSP_ATTRIBUTE_VALUE_TOKEN) {
      return pack(JspHighlighterColors.JSP_ATTRIBUTE_VALUE);
    }
    if (tGSP_DIRECT_TOKENS.contains(tokenType)) {
      return pack(JspHighlighterColors.JSP_ACTION_AND_DIRECTIVE_BACKGROUND);
    }
    return TextAttributesKey.EMPTY_ARRAY;
  }
}
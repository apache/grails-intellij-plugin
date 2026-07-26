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

package org.apache.grails.intellij.plugin.lang.gsp.lexer;

import com.intellij.lexer.FlexAdapter;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.xml.XmlTokenType;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.core._GspLexer;

public class GspFlexLexer extends MergingLexerAdapter {

  private static final TokenSet TOKENS_TO_MERGE = TokenSet.create(GspTokenTypesEx.GSP_TEMPLATE_DATA,
      XmlTokenType.XML_WHITE_SPACE,
                                                                  GspTokenTypes.GROOVY_CODE,
                                                                  GspTokenTypes.GROOVY_DECLARATION,
                                                                  GspTokenTypes.GSP_STYLE_COMMENT,
                                                                  GspTokenTypes.JSP_STYLE_COMMENT,
                                                                  GspTokenTypes.GSP_DIRECTIVE,
                                                                  GspTokenTypes.GSP_ATTRIBUTE_VALUE_TOKEN,
                                                                  GspTokenTypes.GROOVY_EXPR_CODE,
                                                                  GspTokenTypes.GSP_BAD_CHARACTER
  );

  public GspFlexLexer() {
    super(new FlexAdapter(new _GspLexer(null)), TOKENS_TO_MERGE);
  }
}

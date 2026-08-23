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

package org.apache.grails.intellij.plugin.lang.gsp.parsing.gsp.lexer;

import com.intellij.lexer.DelegateLexer;
import com.intellij.psi.tree.IElementType;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.GspFlexLexer;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes;

import static org.apache.grails.intellij.plugin.lang.gsp.lexer.GspTokenTypesEx.GSP_GROOVY_CODE;

public class GspLexer extends DelegateLexer implements GspTokenTypes {
  public GspLexer() {
    super(new GspFlexLexer());
  }

  @Override
  public IElementType getTokenType() {
    return convertToken(super.getTokenType());
  }

  /**
   * Converts token for GSP representation
   */
  private static IElementType convertToken(IElementType tokenType) {
    if (GROOVY_EXPR_CODE == tokenType ||
        GSP_MAP_ATTR_VALUE == tokenType ||
        GROOVY_ATTR_VALUE == tokenType ||
        GROOVY_CODE == tokenType ||
        GROOVY_DECLARATION == tokenType) {
      return GSP_GROOVY_CODE;
    }
    return tokenType;
  }
}

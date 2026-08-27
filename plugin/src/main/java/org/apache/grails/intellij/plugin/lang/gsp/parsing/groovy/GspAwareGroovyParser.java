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

package org.apache.grails.intellij.plugin.lang.gsp.parsing.groovy;

import com.intellij.lang.PsiBuilder;
import com.intellij.psi.tree.IElementType;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.GspTokenTypesEx;
import org.jetbrains.plugins.groovy.lang.parser.GroovyParser;

import static org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes.GROOVY_ATTR_VALUE;
import static org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes.GROOVY_DECLARATION;
import static org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes.GROOVY_EXPR_CODE;
import static org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes.GSP_MAP_ATTR_VALUE;
import static org.apache.grails.intellij.plugin.lang.gsp.parsing.GspGroovyElementTypes.GSP_RUN_BLOCK;

public class GspAwareGroovyParser extends GroovyParser {

  @Override
  protected boolean parse_root_(IElementType t, PsiBuilder b) {
    if (t == GSP_MAP_ATTR_VALUE) {
      return list_or_map(b, 0);
    }
    else if (t == GROOVY_ATTR_VALUE || t == GROOVY_EXPR_CODE) {
      return expression_or_application(b, 0);
    }
    else if (t == GROOVY_DECLARATION) {
      return class_body_inner(b, 0);
    }
    else if (t == GSP_RUN_BLOCK) {
      return block_levels(b, 0);
    }
    else {
      throw new IllegalArgumentException("Unexpected element type: " + t);
    }
  }

  @Override
  public boolean parseDeep() {
    return true;
  }

  @Override
  protected boolean isExtendedSeparator(final IElementType tokenType) {
    return GspTokenTypesEx.GSP_GROOVY_SEPARATORS.contains(tokenType);
  }

  @Override
  protected boolean parseExtendedStatement(PsiBuilder builder) {
    return GspTemplateStmtParsing.parseGspTemplateStmt(builder);
  }
}

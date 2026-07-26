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

package org.apache.grails.intellij.plugin.lang.gsp.parsing.gsp;

import com.intellij.lang.ASTNode;
import com.intellij.lang.ParserDefinition;
import com.intellij.lang.PsiParser;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.xml.XmlTokenType;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.GspLanguage;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.GspTokenTypesEx;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes;
import org.apache.grails.intellij.plugin.lang.gsp.parsing.GspPsiCreator;
import org.apache.grails.intellij.plugin.lang.gsp.parsing.gsp.lexer.GspLexer;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.impl.GspFileImpl;

import static com.intellij.lang.ParserDefinition.SpaceRequirements.MAY;
import static com.intellij.lang.ParserDefinition.SpaceRequirements.MUST_LINE_BREAK;

public final class GspParserDefinition implements ParserDefinition {
  public static final IFileElementType GSP_FILE = new IFileElementType("GSP File", GspLanguage.INSTANCE);

  private static TokenSet whitespaceTokens;

  @Override
  public @NotNull Lexer createLexer(Project project) {
    return new GspLexer() {
      @Override
      public IElementType getTokenType() {
        IElementType type = super.getTokenType();
        if (type == GSP_WHITE_SPACE) return XmlTokenType.XML_WHITE_SPACE;

        if (type == GTAG_START_TAG_START) return XmlTokenType.XML_START_TAG_START;
        if (type == GTAG_END_TAG_START) return XmlTokenType.XML_END_TAG_START;
        if (type == GTAG_START_TAG_END) return XmlTokenType.XML_EMPTY_ELEMENT_END;
        if (type == GTAG_TAG_END) return XmlTokenType.XML_TAG_END;

        if (type == GSP_TAG_NAME) return XmlTokenType.XML_TAG_NAME;
        if (type == GSP_ATTR_NAME) return XmlTokenType.XML_NAME;
        if (type == GSP_ATTRIBUTE_VALUE_TOKEN) return XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN;
        if (type == GSP_ATTR_VALUE_START_DELIMITER) return XmlTokenType.XML_ATTRIBUTE_VALUE_START_DELIMITER;
        if (type == GSP_ATTR_VALUE_END_DELIMITER) return XmlTokenType.XML_ATTRIBUTE_VALUE_END_DELIMITER;
        if (type == GSP_EQ) return XmlTokenType.XML_EQ;
        return type;
      }

    };
  }

  @Override
  public @NotNull PsiParser createParser(Project project) {
    return new GspParser();
  }

  @Override
  public @NotNull IFileElementType getFileNodeType() {
    return GSP_FILE;
  }

  @Override
  public @NotNull TokenSet getWhitespaceTokens() {
    if (whitespaceTokens == null) {
      whitespaceTokens = TokenSet.create(GspTokenTypes.GSP_WHITE_SPACE, XmlTokenType.XML_WHITE_SPACE);
    }
    return whitespaceTokens;
  }

  @Override
  public @NotNull TokenSet getCommentTokens() {
    return GspTokenTypesEx.GSP_COMMENTS;
  }

  @Override
  public @NotNull TokenSet getStringLiteralElements() {
    return TokenSet.EMPTY;
  }

  @Override
  public @NotNull PsiElement createElement(ASTNode node) {
    return GspPsiCreator.createElement(node);
  }

  @Override
  public @NotNull PsiFile createFile(@NotNull FileViewProvider viewProvider) {
    return new GspFileImpl(viewProvider);
  }

  @Override
  public @NotNull SpaceRequirements spaceExistenceTypeBetweenTokens(ASTNode left, ASTNode right) {
    if (GspTokenTypesEx.GSP_GROOVY_SEPARATORS.contains(left.getElementType()) ||
        GspTokenTypesEx.GSP_GROOVY_SEPARATORS.contains(right.getElementType())) {
      return MUST_LINE_BREAK;
    }
    return MAY;
  }
}

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

package org.apache.grails.intellij.plugin.lang.gsp.parsing.groovy.chameleons;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lexer.Lexer;
import com.intellij.lexer.MergingLexerAdapter;
import com.intellij.psi.ParsingDiagnostics;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IFileElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.GspTokenTypesEx;
import org.apache.grails.intellij.plugin.lang.gsp.parsing.groovy.GspGroovyParser;
import org.apache.grails.intellij.plugin.lang.gsp.parsing.groovy.lexer.GspGroovyLexer;
import org.jetbrains.plugins.groovy.GroovyLanguage;

/**
 * Root element of Groovy representation of GSP file
 */
public class GroovyDeclarationsInGspFileRoot extends IFileElementType {
  public GroovyDeclarationsInGspFileRoot(String debugName) {
    super(debugName, GroovyLanguage.INSTANCE);
  }

  private static final TokenSet TOKENS_TO_MERGE = TokenSet.create(GspTokenTypesEx.GSP_TEMPLATE_DATA);

  @Override
  protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
    Lexer lexer = new MergingLexerAdapter(new GspGroovyLexer(), TOKENS_TO_MERGE);
    PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(psi.getProject(), lexer, chameleon);
    var startTime = System.nanoTime();
    var result = new GspGroovyParser().parse(this, builder).getFirstChildNode();
    ParsingDiagnostics.registerParse(builder, getLanguage(), System.nanoTime() - startTime);
    return result;
  }
}

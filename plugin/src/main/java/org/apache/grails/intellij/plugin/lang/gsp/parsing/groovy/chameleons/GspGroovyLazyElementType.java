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
import com.intellij.psi.ParsingDiagnostics;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.ILazyParseableElementType;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.parsing.groovy.GspAwareGroovyParser;
import org.jetbrains.plugins.groovy.GroovyLanguage;

/**
 * Base for the Groovy islands inside a GSP: the contents are reparsed lazily by the GSP-aware
 * Groovy parser rather than by the plain Groovy one.
 */
public abstract class GspGroovyLazyElementType extends ILazyParseableElementType {

  protected GspGroovyLazyElementType(@NotNull String debugName) {
    super(debugName, GroovyLanguage.INSTANCE);
  }

  @Override
  protected ASTNode doParseContents(@NotNull ASTNode chameleon, @NotNull PsiElement psi) {
    PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(psi.getProject(), chameleon);
    long startTime = System.nanoTime();
    ASTNode result = new GspAwareGroovyParser().parse(this, builder).getFirstChildNode();
    ParsingDiagnostics.registerParse(builder, getLanguage(), System.nanoTime() - startTime);
    return result;
  }
}

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

package org.apache.grails.intellij.plugin.lang.gsp.parsing;

import com.intellij.lang.ASTNode;
import com.intellij.psi.tree.ICompositeElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IFileElementType;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.GspTokenTypesEx;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.IGspElementType;
import org.apache.grails.intellij.plugin.lang.gsp.parsing.groovy.chameleons.GroovyDeclarationsInGspFileRoot;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.impl.GrGspClassImpl;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.impl.GrGspRunBlockImpl;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.impl.GrGspRunMethodImpl;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyElementType;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement;

public interface GspGroovyElementTypes extends GspTokenTypesEx {
  IFileElementType GSP_GROOVY_DECLARATIONS_ROOT = new GroovyDeclarationsInGspFileRoot("GROOVY_DECLARATIONS_IN_GSP_FILE");

  IElementType GSP_CLASS = new GroovyElementType.PsiCreator("GSP_CLASS") {
    @Override
    public @NotNull GroovyPsiElement createPsi(@NotNull ASTNode node) {
      return new GrGspClassImpl(node);
    }
  };
  IElementType GSP_RUN_METHOD = new GroovyElementType.PsiCreator("GSP_RUN_METHOD") {
    @Override
    public @NotNull GroovyPsiElement createPsi(@NotNull ASTNode node) {
      return new GrGspRunMethodImpl(node);
    }
  };
  IElementType GSP_RUN_BLOCK = new GspRunBlockElementType();
  
  IElementType GSP_TEMPLATE_STATEMENT = new IGspElementType("GSP_TEMPLATE_STATEMENT");

  class GspRunBlockElementType extends GroovyElementType implements ICompositeElementType {
    public GspRunBlockElementType() {
      super("GSP_RUN_BLOCK");
    }

    @Override
    public @NotNull ASTNode createCompositeNode() {
      return new GrGspRunBlockImpl(this, null);
    }
  }
}

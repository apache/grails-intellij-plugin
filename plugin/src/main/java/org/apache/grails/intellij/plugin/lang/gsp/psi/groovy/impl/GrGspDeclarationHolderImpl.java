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

package org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.impl;

import com.intellij.psi.PsiElement;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.GspLazyElement;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspClass;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspDeclarationHolder;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;

import java.util.ArrayList;
import java.util.List;

import static org.jetbrains.plugins.groovy.lang.psi.util.PsiTreeUtilKt.treeWalkUp;

public class GrGspDeclarationHolderImpl extends GspLazyElement implements GrGspDeclarationHolder {

  public GrGspDeclarationHolderImpl(@NotNull IElementType type, @Nullable CharSequence buffer) {
    super(type, buffer);
  }

  @Override
  public String toString() {
    return "Groovy class level declaration element";
  }

  @Override
  public void accept(@NotNull GroovyElementVisitor visitor) {
  }

  @Override
  public GrField[] getFields() {
    GrVariableDeclaration[] declarations = findChildrenByClass(GrVariableDeclaration.class);
    if (declarations.length == 0) return GrField.EMPTY_ARRAY;
    List<GrField> result = new ArrayList<>();
    for (GrVariableDeclaration declaration : declarations) {
      GrVariable[] variables = declaration.getVariables();
      for (GrVariable variable : variables) {
        if (variable instanceof GrField) {
          result.add((GrField) variable);
        }
      }
    }
    return result.toArray(GrField.EMPTY_ARRAY);
  }


  @Override
  public GrMethod[] getMethods() {
    return findChildrenByClass(GrMethod.class);
  }

  @Override
  public boolean processDeclarations(@NotNull PsiScopeProcessor processor, @NotNull ResolveState state, PsiElement lastParent, @NotNull PsiElement place) {
    GrGspClass clazz = PsiTreeUtil.getParentOfType(this, GrGspClass.class);
    if (clazz != null) {
      if (!clazz.processDeclarations(processor, state, this, place)) return false;
      treeWalkUp(clazz, processor);
    }

    return false; //do not attempt any further resolving
  }
}

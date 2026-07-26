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

package org.jetbrains.plugins.grails.references.common;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;

import java.util.ArrayList;
import java.util.List;

public class GroovyGspTagWrapper implements GspTagWrapper {

  private final GrNamedArgumentsOwner myNamedArgumentsOwner;
  private final TagLibNamespaceDescriptor.GspTagMethod myGspTagLibVariable;

  public GroovyGspTagWrapper(GrNamedArgumentsOwner namedArgumentsOwner, TagLibNamespaceDescriptor.GspTagMethod gspTagLibVariable) {
    myNamedArgumentsOwner = namedArgumentsOwner;
    this.myGspTagLibVariable = gspTagLibVariable;
  }

  @Override
  public @NotNull String getTagName() {
    return myGspTagLibVariable.getTagName();
  }

  @Override
  public boolean hasAttribute(@NotNull String name) {
    return myNamedArgumentsOwner.findNamedArgument(name) != null;
  }

  @Override
  public GrExpression getAttributeValue(@NotNull String name) {
    GrNamedArgument argument = myNamedArgumentsOwner.findNamedArgument(name);
    if (argument == null) return null;
    return argument.getExpression();
  }

  @Override
  public PsiType getAttributeValueType(@NotNull String name) {
    GrExpression expression = getAttributeValue(name);
    if (expression == null) return null;

    return expression.getType();
  }

  @Override
  public String getAttributeText(@NotNull PsiElement attributeValue) {
    if (!(attributeValue instanceof GrLiteralImpl literal)) return null;

    Object value = literal.getValue();
    if (!(value instanceof String)) return null;

    return (String)value;
  }

  @Override
  public List<String> getAttributeNames() {
    List<String> res = new ArrayList<>();

    for (GrNamedArgument argument : myNamedArgumentsOwner.getNamedArguments()) {
      ContainerUtil.addIfNotNull(res, argument.getLabelName());
    }
    
    return res;
  }
}

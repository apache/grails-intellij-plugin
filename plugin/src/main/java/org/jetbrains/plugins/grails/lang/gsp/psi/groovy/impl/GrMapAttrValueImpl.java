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

package org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl;

import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.GspLazyElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GrMapAttributeValue;
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrStatement;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;

public class GrMapAttrValueImpl extends GspLazyElement implements GrMapAttributeValue {

  public GrMapAttrValueImpl(@NotNull IElementType type, @Nullable CharSequence buffer) {
    super(type, buffer);
  }

  @Override
  public String toString() {
    return "GrMapAttrValueImpl";
  }

  @Override
  public void accept(@NotNull GroovyElementVisitor visitor) {
    visitor.visitStatement(this);
  }

  @Override
  public @Nullable GrExpression getExpression() {
    return findChildByClass(GrExpression.class);
  }

  @Override
  public @Nullable <T extends GrStatement> T replaceWithStatement(T statement) {
    return null;
  }

  @Override
  public void removeStatement() {}
}

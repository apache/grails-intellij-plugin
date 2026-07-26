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

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.directive;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.XmlAttributeImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttribute;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttributeValue;

public class GspDirectiveAttributeImpl extends XmlAttributeImpl implements GspDirectiveAttribute {
  public GspDirectiveAttributeImpl() {
    super();
  }

  @Override
  public @NotNull IElementType getElementType() {
    return GspElementTypes.GSP_DIRECTIVE_ATTRIBUTE;
  }

  @Override
  public XmlAttributeValue getValueElement() {
    ASTNode node = findChildByType(GspElementTypes.GSP_DIRECTIVE_ATTRIBUTE_VALUE);
    if (node != null) {
      PsiElement psi = node.getPsi();
      assert psi instanceof GspDirectiveAttributeValue;
      return (GspDirectiveAttributeValue) psi;
    }
    return null;
  }

  @Override
  public String toString() {
    return "GSP_DIRECTIVE_ATTRIBUTE";
  }

}

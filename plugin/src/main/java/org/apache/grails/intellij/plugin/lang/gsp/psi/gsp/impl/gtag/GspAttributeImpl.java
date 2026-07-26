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

package org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.xml.XmlAttributeDelegate;
import com.intellij.psi.impl.source.xml.XmlAttributeImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.parsing.GspElementTypes;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.gtag.GspAttribute;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.gtag.GspAttributeValue;

public class GspAttributeImpl extends XmlAttributeImpl implements GspAttribute {

  @Override
  public String toString() {
    return "GSP attribute";
  }

  @Override
  public @NotNull IElementType getElementType() {
    return GspElementTypes.GRAILS_TAG_ATTRIBUTE;
  }

  @Override
  public XmlAttributeValue getValueElement() {
    ASTNode node = findChildByType(GspElementTypes.GSP_ATTRIBUTE_VALUE);
    if (node != null) {
      PsiElement psi = node.getPsi();
      assert psi instanceof GspAttributeValue;
      return (GspAttributeValue)psi;
    }
    return null;
  }

  @Override
  protected @NotNull XmlAttributeDelegate createDelegate() {
    return new GspAttributeImplDelegate();
  }

  private class GspAttributeImplDelegate extends XmlAttributeImplDelegate {
    @Override
    protected void appendChildToDisplayValue(@NotNull StringBuilder buffer, @NotNull ASTNode child) {
      //super.appendChildToDisplayValue(buffer, child);
      CharSequence sq = child.getChars();
      boolean slash = false;
      for (int i = 0; i < sq.length(); i++) {
        char a = sq.charAt(i);

        if (slash) {
          buffer.append(a);
          slash = false;
        }
        else {
          if (a == '\\') {
            slash = true;
          }
          else {
            buffer.append(a);
          }
        }
      }
    }
  }
}

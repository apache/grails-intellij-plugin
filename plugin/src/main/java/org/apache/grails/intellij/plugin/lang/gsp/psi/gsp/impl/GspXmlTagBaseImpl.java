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

package org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.impl;

import com.intellij.lang.ASTNode;
import com.intellij.psi.impl.source.tree.TreeElement;
import com.intellij.psi.impl.source.xml.XmlTagImpl;
import com.intellij.psi.impl.source.xml.XmlTagValueImpl;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlTagChild;
import com.intellij.psi.xml.XmlTagValue;
import com.intellij.psi.xml.XmlText;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.GspLanguage;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspXmlTagBase;

public abstract class GspXmlTagBaseImpl extends XmlTagImpl implements GspXmlTagBase {

  public GspXmlTagBaseImpl(IElementType type) {
    super(type);
  }

  @Override
  public XmlTag findParentTag() {
    return PsiTreeUtil.getParentOfType(this, XmlTag.class);
  }

  @Override
  public @NotNull XmlTagValue getValue() {
    final XmlText xmlText = PsiTreeUtil.getChildOfType(this, XmlText.class);

    if (xmlText == null) return new XmlTagValueImpl(EMPTY, this);
    return new XmlTagValueImpl(new XmlTagChild[]{xmlText}, this);
  }

  @Override
  public TreeElement addInternal(TreeElement first, ASTNode last, ASTNode anchor, Boolean before) {
    if (anchor == null &&
        getLastChildNode().getElementType().getLanguage() == GspLanguage.INSTANCE)
      return super.addInternal(first, last, getLastChildNode(), Boolean.TRUE);
    return super.addInternal(first, last, anchor, before);
  }
}

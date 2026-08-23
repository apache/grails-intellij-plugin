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

package org.apache.grails.intellij.plugin.references.common;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsPsiUtil;

import java.util.ArrayList;
import java.util.List;

public class XmlGspTagWrapper implements GspTagWrapper {

  private final XmlTag myTag;

  public XmlGspTagWrapper(XmlTag tag) {
    myTag = tag;
  }

  @Override
  public @NotNull String getTagName() {
    return myTag.getName();
  }

  @Override
  public boolean hasAttribute(@NotNull String name) {
    return myTag.getAttribute(name) != null;
  }

  @Override
  public XmlAttributeValue getAttributeValue(@NotNull String name) {
    XmlAttribute attrValue = myTag.getAttribute(name);
    if (attrValue == null) return null;
    return attrValue.getValueElement();
  }

  @Override
  public PsiType getAttributeValueType(@NotNull String name) {
    XmlAttributeValue attribute = getAttributeValue(name);
    if (attribute == null) return null;
    return GrailsPsiUtil.getAttributeExpressionType(attribute);
  }

  @Override
  public String getAttributeText(@NotNull PsiElement element) {
    XmlAttributeValue attributeValue = (XmlAttributeValue)element;
    if (!GrailsPsiUtil.isSimpleAttribute(attributeValue)) return null;
    return attributeValue.getValue();
  }

  @Override
  public List<String> getAttributeNames() {
    XmlAttribute[] attributes = myTag.getAttributes();
    List<String> res = new ArrayList<>(attributes.length);
    
    for (XmlAttribute attribute : attributes) {
      res.add(attribute.getName());
    }
    
    return res;
  }
}

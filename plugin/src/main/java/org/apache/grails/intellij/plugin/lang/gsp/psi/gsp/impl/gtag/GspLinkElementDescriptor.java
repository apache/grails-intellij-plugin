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

package org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlAttribute;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.XmlElementDescriptor;
import com.intellij.xml.XmlElementsGroup;
import com.intellij.xml.XmlNSDescriptor;
import com.intellij.xml.impl.BasicXmlAttributeDescriptor;
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class GspLinkElementDescriptor implements XmlElementDescriptor {
  private static final XmlAttributeDescriptor ATTR_DESCRIPTOR = new AnyXmlAttributeDescriptor("attrs");

  private final String myName;
  private final String myLocalName;
  private final PsiElement myDescriptor;

  private final Map<String, XmlAttributeDescriptor> myAttributeDescriptors;

  private final XmlAttributeDescriptor[] myAttributes;

  public GspLinkElementDescriptor(String localName, PsiElement descriptor, Map<String, Pair<PsiElement, Boolean>> params) {
    myName = GspLinkNamespaceDescriptor.NAMESPACE_LINK + ':' + localName;
    myLocalName = localName;
    myDescriptor = descriptor;

    if (params.isEmpty()) {
      myAttributeDescriptors = Collections.singletonMap(ATTR_DESCRIPTOR.getName(), ATTR_DESCRIPTOR);
      myAttributes = new XmlAttributeDescriptor[]{ATTR_DESCRIPTOR};
    }
    else {
      myAttributeDescriptors = new HashMap<>();
      myAttributes = new XmlAttributeDescriptor[params.size() + 1];

      myAttributes[0] = ATTR_DESCRIPTOR;
      myAttributeDescriptors.put(ATTR_DESCRIPTOR.getName(), ATTR_DESCRIPTOR);

      int i = 1;
      for (Map.Entry<String, Pair<PsiElement, Boolean>> entry : params.entrySet()) {
        XmlAttributeDescriptor descr = new LinkAttributeDescriptor(entry.getKey(), entry.getValue().first, entry.getValue().second);

        myAttributeDescriptors.put(entry.getKey(), descr);
        myAttributes[i++] = descr;
      }
    }
  }

  @Override
  public String getQualifiedName() {
    return myName;
  }

  @Override
  public String getDefaultName() {
    return myName;
  }

  @Override
  public XmlElementDescriptor[] getElementsDescriptors(XmlTag context) {
    return EMPTY_ARRAY;
  }

  @Override
  public XmlElementDescriptor getElementDescriptor(XmlTag childTag, XmlTag contextTag) {
    return null;
  }

  @Override
  public XmlAttributeDescriptor[] getAttributesDescriptors(@Nullable XmlTag context) {
    return myAttributes;
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
    XmlAttributeDescriptor res = myAttributeDescriptors.get(attributeName);
    if (res == null) {
      res = new AnyXmlAttributeDescriptor(attributeName);
    }
    return res;
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(XmlAttribute attribute) {
    return getAttributeDescriptor(attribute.getName(), null);
  }

  @Override
  public XmlNSDescriptor getNSDescriptor() {
    return GspLinkNamespaceDescriptor.INSTANCE;
  }

  @Override
  public XmlElementsGroup getTopGroup() {
    return null;
  }

  @Override
  public int getContentType() {
    return CONTENT_TYPE_EMPTY;
  }

  @Override
  public String getDefaultValue() {
    return null;
  }

  @Override
  public PsiElement getDeclaration() {
    return myDescriptor;
  }

  @Override
  public String getName(PsiElement context) {
    return myName;
  }

  @Override
  public String getName() {
    return myLocalName;
  }

  @Override
  public void init(PsiElement element) {
    throw new UnsupportedOperationException("Method init is not yet implemented in " + getClass().getName());
  }

  @Override
  public Object @NotNull [] getDependencies() {
    throw new UnsupportedOperationException("Method getDependencies is not yet implemented in " + getClass().getName());
  }

  private static class LinkAttributeDescriptor extends BasicXmlAttributeDescriptor {

    private final String name;
    private final PsiElement element;
    private final boolean myOptional;

    LinkAttributeDescriptor(String name, PsiElement element, boolean isOptional) {
      this.name = name;
      this.element = element;
      myOptional = isOptional;
    }

    @Override
    public boolean isRequired() {
      return !myOptional;
    }

    @Override
    public boolean isFixed() {
      return false;
    }

    @Override
    public boolean hasIdType() {
      return false;
    }

    @Override
    public boolean hasIdRefType() {
      return false;
    }

    @Override
    public String getDefaultValue() {
      return null;
    }

    @Override
    public boolean isEnumerated() {
      return false;
    }

    @Override
    public String[] getEnumeratedValues() {
      throw new RuntimeException();
    }

    @Override
    public PsiElement getDeclaration() {
      return element;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void init(PsiElement element) {
      throw new UnsupportedOperationException("Method init is not yet implemented in " + getClass().getName());
    }

    @Override
    public Object @NotNull [] getDependencies() {
      throw new UnsupportedOperationException("Method getDependencies is not yet implemented in " + getClass().getName());
    }
  }
}

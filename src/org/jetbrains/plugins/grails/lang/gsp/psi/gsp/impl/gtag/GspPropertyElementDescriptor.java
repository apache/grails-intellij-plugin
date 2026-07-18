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
package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.gtag;

import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.xml.XmlTag;
import com.intellij.xml.XmlAttributeDescriptor;
import com.intellij.xml.impl.schema.AnyXmlAttributeDescriptor;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.GspTagLibUtil;

import java.util.Map;
import java.util.Set;

public class GspPropertyElementDescriptor extends GspElementDescriptorBase {
  public GspPropertyElementDescriptor(GspNamespaceDescriptor nsDescriptor, PsiElement place, String localName) {
    super(nsDescriptor, place, localName);
  }

  @Override
  public XmlAttributeDescriptor[] getAttributesDescriptors(final @Nullable XmlTag context) {
    Pair<Map<String,XmlAttributeDescriptor>,Set<String>> pair = GspTagLibUtil.getAttributesDescriptorsFromJavadocs(myPlace);

    Map<String, XmlAttributeDescriptor> javadocDescriptorsMap = pair.first;

    XmlAttributeDescriptor[] res = new XmlAttributeDescriptor[javadocDescriptorsMap.size() + pair.second.size()];

    int i = 0;
    for (XmlAttributeDescriptor descriptor : javadocDescriptorsMap.values()) {
      res[i++] = descriptor;
    }

    for (String attrName : pair.second) {
      res[i++] = new AnyXmlAttributeDescriptor(attrName);
    }

    return res;
  }

  @Override
  public XmlAttributeDescriptor getAttributeDescriptor(@NonNls String attributeName, @Nullable XmlTag context) {
    XmlAttributeDescriptor descriptor = GspTagLibUtil.getAttributesDescriptorsFromJavadocs(myPlace).first.get(attributeName);
    if (descriptor != null) return descriptor;

    return new AnyXmlAttributeDescriptor(attributeName);
  }

}

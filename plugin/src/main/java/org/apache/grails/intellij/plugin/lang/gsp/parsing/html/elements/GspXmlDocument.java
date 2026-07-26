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

package org.apache.grails.intellij.plugin.lang.gsp.parsing.html.elements;

import com.intellij.psi.impl.source.xml.XmlDocumentImpl;
import com.intellij.psi.xml.XmlElementType;
import com.intellij.psi.xml.XmlProlog;
import com.intellij.psi.xml.XmlTag;
import org.apache.grails.intellij.plugin.lang.gsp.parsing.GspElementTypes;

public class GspXmlDocument extends XmlDocumentImpl {
  public GspXmlDocument() {
    super(GspElementTypes.GSP_XML_DOCUMENT);
  }

  @Override
  public XmlProlog getProlog() {
    return (XmlProlog) findElementByTokenType(XmlElementType.XML_PROLOG);
  }

  @Override
  public XmlTag getRootTag() {
    return (XmlTag) findElementByTokenType(GspElementTypes.GSP_ROOT_TAG);
  }

  @Override
  public String toString() {
    return "PsiElement" + "(" + XmlElementType.XML_DOCUMENT.toString() + ")";
  }
}
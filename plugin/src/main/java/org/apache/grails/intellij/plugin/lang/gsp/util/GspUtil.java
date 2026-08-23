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

package org.apache.grails.intellij.plugin.lang.gsp.util;

import com.intellij.lang.ASTNode;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.lang.gsp.GspFileViewProvider;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspFile;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;
import org.apache.grails.intellij.plugin.lang.gsp.psi.html.impl.GspHtmlFileImpl;

public final class GspUtil {

  private GspUtil() {
  }

  public static @Nullable GspGrailsTag getContainingGrailsTag(PsiElement e) {
    GspGrailsTag tag = null;
    if (e == null) return null;
    if (e.getTextOffset() >= 0) {
      FileViewProvider provider = e.getContainingFile().getViewProvider();
      PsiFile file = provider.getPsi(provider.getBaseLanguage());
      if (file instanceof GspFile) {
        int offset = e.getTextRange().getStartOffset();
        ASTNode node = file.getNode().findLeafElementAt(offset);
        if (node != null) {
          PsiElement start = node.getPsi();
          while (start != null && !(start instanceof GspGrailsTag)) {
            start = start.getParent();
          }
          if (start != null) tag = ((GspGrailsTag) start);
        }
      }
    }
    return tag;
  }

  public static @Nullable XmlTag getContainingHtmlTag(PsiElement e) {
    XmlTag tag = null;
    if (e == null) return null;
    if (e.getTextOffset() >= 0) {
      FileViewProvider provider = e.getContainingFile().getViewProvider();
      if (provider instanceof GspFileViewProvider) {
      PsiFile file = provider.getPsi(((GspFileViewProvider) provider).getTemplateDataLanguage());
        if (file instanceof GspHtmlFileImpl) {
          int offset = e.getTextRange().getStartOffset();
          ASTNode node = file.getNode().findLeafElementAt(offset);
          if (node != null) {
            PsiElement start = node.getPsi();
            while (start != null && !(start instanceof XmlTag)) {
              start = start.getParent();
            }
            if (start != null) tag = ((XmlTag) start);
          }
        }
      }
    }
    if (tag != null && tag.getTextRange().contains(e.getTextRange())) return tag;
    return null;
  }
}

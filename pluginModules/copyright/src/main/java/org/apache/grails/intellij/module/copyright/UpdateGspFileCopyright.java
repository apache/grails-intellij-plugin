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

package org.apache.grails.intellij.module.copyright;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.intellij.psi.xml.XmlToken;
import com.maddyhome.idea.copyright.CopyrightProfile;
import com.maddyhome.idea.copyright.options.XmlOptions;
import com.maddyhome.idea.copyright.psi.UpdatePsiFileCopyright;
import org.apache.grails.intellij.plugin.fileType.GspFileType;

/**
 * Places copyright comments in GSP files.
 *
 * <p>Derived from the platform's {@code com.intellij.javaee.jsp.copyright.UpdateJspFileCopyright}.
 * That class lives in the JSP plugin's {@code intellij.javaee.jsp.copyright} content module, which
 * ties this module to javaee/JSP even though none of the JSP-specific logic applies to GSP:
 * {@code accept()} tested for {@code JspFile} (GSP files are {@link org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspFile},
 * not {@code JspFile}), and the root-tag scan skipped {@code JspDirective} tags, which a GSP tree
 * never contains — GSP {@code <%@ page %>} directives are {@code GspDirectiveImpl extends XmlTagImpl}.
 * Both checks were therefore inert here, so dropping them keeps behavior identical while leaving
 * only the always-bundled {@code com.intellij.copyright} plugin and platform XML PSI as dependencies.
 */
class UpdateGspFileCopyright extends UpdatePsiFileCopyright {

  private static final Logger LOG = Logger.getInstance(UpdateGspFileCopyright.class);

  UpdateGspFileCopyright(Project project, Module module, VirtualFile root, CopyrightProfile options) {
    super(project, module, root, options);
  }

  @Override
  protected boolean accept() {
    return getFile().getFileType() == GspFileType.GSP_FILE_TYPE;
  }

  @Override
  protected void scanFile() {
    LOG.debug("updating " + getFile().getVirtualFile());

    // Unlike a JspFile, a GspFile yields a null document when its first AST child is not an
    // XmlDocument, so this cannot be dereferenced blindly the way the JSP original did.
    XmlDocument doc = ((XmlFile)getFile()).getDocument();
    if (doc == null) return;

    XmlTag root = doc.getRootTag();
    if (root == null) return;

    PsiElement elem = root.getFirstChild();
    PsiElement docTypeStart = null;
    PsiElement docTypeEnd = null;
    PsiElement firstTag = null;
    while (elem != null) {
      if (elem instanceof XmlToken) {
        if ("<!DOCTYPE".equals(elem.getText())) {
          docTypeStart = elem;
          while ((elem = getNextSibling(elem)) != null) {
            if (elem instanceof PsiWhiteSpace) continue;
            if (elem instanceof XmlToken) {
              if (elem.getText().endsWith(">")) {
                elem = getNextSibling(elem);
                docTypeEnd = elem;
                break;
              }
              else if (elem.getText().startsWith("<")) {
                docTypeEnd = elem;
                break;
              }
            }
            else {
              break;
            }
          }
          continue;
        }
        else {
          firstTag = elem;
          break;
        }
      }
      else if (elem instanceof XmlTag) {
        firstTag = elem;
        break;
      }
      elem = getNextSibling(elem);
    }

    PsiElement first = root.getFirstChild();

    int location = getLanguageOptions().getFileLocation();
    if (docTypeStart != null) {
      checkComments(first, docTypeStart, location == XmlOptions.LOCATION_BEFORE_DOCTYPE);
      first = docTypeEnd;
    }
    else if (location == XmlOptions.LOCATION_BEFORE_DOCTYPE) {
      location = XmlOptions.LOCATION_BEFORE_ROOTTAG;
    }

    if (firstTag != null) {
      checkComments(first, firstTag, location == XmlOptions.LOCATION_BEFORE_ROOTTAG);
    }
    else if (location == XmlOptions.LOCATION_BEFORE_ROOTTAG) {
      // If we get here we have an empty file
      checkComments(first, first, true);
    }
  }

  @Override
  protected PsiElement getPreviousSibling(PsiElement element) {
    if (element == null) return null;

    PsiElement res = element.getPrevSibling();
    if (res == null) {
      if (element.getParent() instanceof XmlText) {
        res = element.getParent().getPrevSibling();
      }
    }

    if (res instanceof XmlText text) {
      if (text.getLastChild() != null) {
        res = text.getLastChild();
      }
      else {
        res = text.getPrevSibling();
      }
    }

    return res;
  }

  @Override
  protected PsiElement getNextSibling(PsiElement element) {
    if (element == null) return null;

    PsiElement res = element instanceof XmlText ? element.getFirstChild() : element.getNextSibling();
    if (res instanceof XmlText) {
      if (res.getFirstChild() != null) {
        res = res.getFirstChild();
      }
      else {
        res = res.getNextSibling();
      }
    }

    if (res == null) {
      if (element.getParent() instanceof XmlText) {
        res = element.getParent().getNextSibling();
      }
    }

    return res;
  }
}

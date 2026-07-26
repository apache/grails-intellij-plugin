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

package org.apache.grails.intellij.plugin.inspections;

import com.intellij.codeInspection.DefaultXmlSuppressionProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.xml.XmlDocument;
import com.intellij.psi.xml.XmlFile;
import com.intellij.psi.xml.XmlTag;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspFile;

final class GspSuppressionProvider extends DefaultXmlSuppressionProvider {
  @Override
  public boolean isProviderAvailable(@NotNull PsiFile file) {
    return file instanceof GspFile;
  }

  @Override
  protected PsiElement findFileSuppression(PsiElement anchor, String id, PsiElement originalElement) {
    final PsiFile file = anchor.getContainingFile();
    if (file instanceof XmlFile) {
      final XmlDocument document = ((XmlFile)file).getDocument();
      final XmlTag rootTag = document != null ? document.getRootTag() : null;
      PsiElement leaf = rootTag != null ? rootTag.getFirstChild() : file.findElementAt(0);
      return findSuppressionLeaf(leaf, id, 0);
    }
    return null;
  }

  @Override
  protected String getPrefix() {
    return "<%--" +
           DefaultXmlSuppressionProvider.SUPPRESS_MARK +
           " ";
  }

  @Override
  protected String getSuffix() {
    return " --%>";
  }
}

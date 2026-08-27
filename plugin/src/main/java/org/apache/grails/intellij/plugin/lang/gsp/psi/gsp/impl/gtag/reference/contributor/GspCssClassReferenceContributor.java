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

package org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.impl.gtag.reference.contributor;

import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.css.CssElementDescriptorProvider;
import com.intellij.psi.css.impl.util.table.CssDescriptorsUtilCore;
import com.intellij.psi.impl.PsiImplUtil;
import com.intellij.psi.xml.XmlAttributeValue;
import com.intellij.psi.xml.XmlTokenType;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsPatterns;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class GspCssClassReferenceContributor extends PsiReferenceContributor {
  private static class Holder {
    private static final Pattern CLASS_NAME_PATTERN = Pattern.compile("\\S+");
  }

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    registrar.registerReferenceProvider(
      GrailsPatterns.gspAttributeValue(XmlPatterns.xmlAttribute("class").withParent(XmlPatterns.xmlTag().withNamespace("g"))),
      new PsiReferenceProvider() {
        @Override
        public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
          // Create css reference like CssInHtmlClassOrIdReferenceProvider

          if (!(element instanceof XmlAttributeValue)) return PsiReference.EMPTY_ARRAY;

          List<PsiReference> res = new ArrayList<>();
          CssElementDescriptorProvider descriptorProvider = CssDescriptorsUtilCore.findDescriptorProvider(element);
          if (descriptorProvider == null) return PsiReference.EMPTY_ARRAY;

          for (PsiElement child = element.getFirstChild(); child != null; child = child.getNextSibling()) {
            if (!PsiImplUtil.isLeafElementOfType(child, XmlTokenType.XML_ATTRIBUTE_VALUE_TOKEN)) continue;

            Matcher matcher = Holder.CLASS_NAME_PATTERN.matcher(child.getText());
            while (matcher.find()) {
              int offsetChild = child.getStartOffsetInParent();
              res.add(descriptorProvider.getStyleReference(element, offsetChild + matcher.start(), offsetChild + matcher.end(), false));
            }
          }

          return res.toArray(PsiReference.EMPTY_ARRAY);
        }
      }
    );
  }
}

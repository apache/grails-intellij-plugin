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
package org.apache.grails.intellij.plugin.addins.js;

import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.lang.javascript.JavascriptLanguage;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiLanguageInjectionHost;
import com.intellij.psi.xml.XmlAttributeValue;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspExpressionTag;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspScriptletTag;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.gtag.GspAttribute;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;
import org.apache.grails.intellij.plugin.util.GrailsPsiUtil;

import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

 final class GrailsJavaScriptInjector implements MultiHostInjector {
  private static Pattern ATTRIBUTE_PATTERN;

  public static Pattern getAttributePattern() {
    if (ATTRIBUTE_PATTERN == null) {
      ATTRIBUTE_PATTERN = Pattern.compile("on([A-Z][a-z]+|_\\d+|blur|click|change|dblclick|focus|keydown|keypress|keyup|mousedown|mousemove|mouseout|mouseover|mouseup|submit)");
    }
    return ATTRIBUTE_PATTERN;
  }

  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar originalRegistrar, final @NotNull PsiElement eTag) {
    GspGrailsTag tag = (GspGrailsTag)eTag;

    String tagName = tag.getName();

    if (JavaScriptIntegrationUtil.isJsInjectionTag(tagName)) {
      MultiHostRegistrar currentRegistrar = null;

      PsiElement child = tag.getFirstChild();
      while (child != null) {
        PsiElement nextSibling = child.getNextSibling();

        if (JavaScriptIntegrationUtil.isInjectAvailable(child)) {
          if (currentRegistrar == null) {
            currentRegistrar = originalRegistrar.startInjecting(JavascriptLanguage.INSTANCE);
          }

          String suffix = nextSibling instanceof GspExpressionTag || nextSibling instanceof GspScriptletTag ? " fpwmx8th2p8v2 " : null;

          currentRegistrar = currentRegistrar.addPlace(null, suffix, (PsiLanguageInjectionHost)child, new TextRange(0, child.getTextLength()));
        }

        child = nextSibling;
      }

      if (currentRegistrar != null) {
        currentRegistrar.doneInjecting();
      }
    }
    else {
      boolean hasBeforeAndAfter = tagName.equals("g:formRemote") || tagName.equals("g:remoteField") || tagName.equals("g:remoteFunction")
                                  || tagName.equals("g:remoteLink") || tagName.equals("g:submitToRemote");

      for (PsiElement child = tag.getFirstChild(); child != null; child = child.getNextSibling()) {
        if (child instanceof GspAttribute attribute) {

          String name = attribute.getName();

          if (getAttributePattern().matcher(name).matches() || (hasBeforeAndAfter && (name.equals("before") || name.equals("after")))) {
            XmlAttributeValue value = attribute.getValueElement();
            if (value != null) {
              int length = value.getTextLength();
              if (length >= 2) {
                if (length == 2 || GrailsPsiUtil.isSimpleAttribute(value)) {
                  MultiHostRegistrar currentRegistrar = originalRegistrar.startInjecting(JavascriptLanguage.INSTANCE);

                  currentRegistrar = currentRegistrar.addPlace(null, null, (PsiLanguageInjectionHost)value,
                                                               new TextRange(1, length - 1));
                  currentRegistrar.doneInjecting();
                }
              }
            }
          }
        }
      }
    }
  }

  @Override
  public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(GspGrailsTag.class);
  }
}

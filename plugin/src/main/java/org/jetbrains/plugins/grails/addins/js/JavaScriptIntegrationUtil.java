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

package org.jetbrains.plugins.grails.addins.js;

import com.intellij.lang.javascript.JSElementType;
import com.intellij.lang.javascript.psi.JSElement;
import com.intellij.lang.javascript.psi.JSEmbeddedContent;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.plugins.grails.addins.GrailsIntegrationUtil;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterHtmlElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;

import java.util.concurrent.atomic.AtomicLong;

public final class JavaScriptIntegrationUtil {

  private static final String JS_GRAILS_TAG_NAME1 = "g:javascript";
  private static final String JS_GRAILS_TAG_NAME2 = "r:script";

  // Bumped whenever the set of JS-injection-tag contributions changes (dynamic plugin/module
  // load-unload). Consumers that cache tag lists (e.g. GspFileImpl) compare against this to
  // invalidate; see GspFileImpl.getJsTags().
  private static final AtomicLong ourInjectionTagEpModCount = new AtomicLong();

  static {
    GspJsInjectionTagBean.EP_NAME.addChangeListener(ourInjectionTagEpModCount::incrementAndGet, null);
  }

  private JavaScriptIntegrationUtil() {
  }

  public static long getInjectionTagModificationCount() {
    return ourInjectionTagEpModCount.get();
  }

  public static boolean isJSEmbeddedContent(final PsiElement element) {
    return element instanceof JSEmbeddedContent;
  }

  public static boolean isJSElementType(IElementType type) {
    return GrailsIntegrationUtil.isJsSupportEnabled() && type instanceof JSElementType;
  }

  public static boolean isJSElement(PsiElement element) {
    return GrailsIntegrationUtil.isJsSupportEnabled() && element instanceof JSElement;
  }

  public static boolean isInjectAvailable(PsiElement element) {
    return element instanceof GspOuterHtmlElement;
  }

  public static boolean isJsInjectionTag(String tagName) {
    if (JS_GRAILS_TAG_NAME1.equals(tagName) || JS_GRAILS_TAG_NAME2.equals(tagName)) {
      return true;
    }
    for (GspJsInjectionTagBean bean : GspJsInjectionTagBean.EP_NAME.getExtensionList()) {
      if (tagName.equals(bean.name)) return true;
    }
    return false;
  }

  public static boolean isJavaScriptInjection(PsiElement element) {
    if (isInjectAvailable(element) && GrailsIntegrationUtil.isJsSupportEnabled()) {
      PsiElement parent = element.getParent();
      if (!(parent instanceof GspGrailsTag)) return false;

      return isJsInjectionTag(((GspGrailsTag)parent).getName());
    }
    return false;
  }
}

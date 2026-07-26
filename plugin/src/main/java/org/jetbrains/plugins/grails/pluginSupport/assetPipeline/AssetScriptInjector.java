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
package org.jetbrains.plugins.grails.pluginSupport.assetPipeline;

import com.intellij.lang.Language;
import com.intellij.lang.LanguageUtil;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.html.HtmlScriptLanguageInjector;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.addins.js.JavaScriptIntegrationUtil;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterHtmlElement;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag;

import java.util.List;

public final class AssetScriptInjector implements MultiHostInjector {

  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement context) {
    GspGrailsTag tag = (GspGrailsTag)context;
    if (!"asset".equals(tag.getNamespacePrefix())) return;
    // JavaScript-body tags (e.g. asset:script) are owned by the core JS injector via
    // JavaScriptIntegrationUtil.isJsInjectionTag, which also grants cross-tag symbol visibility;
    // injecting here too would double-inject the same host.
    if (JavaScriptIntegrationUtil.isJsInjectionTag(tag.getName())) return;
    Language languageToInject = HtmlScriptLanguageInjector.getScriptLanguageToInject(tag);
    if (languageToInject == null) return;
    if (!LanguageUtil.isInjectableLanguage(languageToInject)) return;
    boolean started = false;
    for (PsiElement child : tag.getChildren()) {
      if (child instanceof GspOuterHtmlElement outerHtml) {
        if (!started) {
          registrar.startInjecting(languageToInject);
          started = true;
        }
        registrar.addPlace(null, null, outerHtml, TextRange.create(0, outerHtml.getTextLength()));
      }
    }
    if (started) registrar.doneInjecting();
  }

  @Override
  public @NotNull List<? extends Class<? extends PsiElement>> elementsToInjectIn() {
    return List.of(GspGrailsTag.class);
  }
}

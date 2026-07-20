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

package org.jetbrains.plugins.grails.pluginSupport.assetPipeline

import com.intellij.lang.LanguageUtil
import com.intellij.lang.injection.MultiHostInjector
import com.intellij.lang.injection.MultiHostRegistrar
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.html.HtmlScriptLanguageInjector
import org.jetbrains.plugins.grails.addins.js.JavaScriptIntegrationUtil
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.api.GspOuterHtmlElement
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.gtag.GspGrailsTag

class AssetScriptInjector : MultiHostInjector {

  override fun getLanguagesToInject(registrar: MultiHostRegistrar, context: PsiElement) {
    context as GspGrailsTag
    if (context.namespacePrefix != "asset") return
    // JavaScript-body tags (e.g. asset:script) are owned by the core JS injector via
    // JavaScriptIntegrationUtil.isJsInjectionTag, which also grants cross-tag symbol visibility;
    // injecting here too would double-inject the same host.
    if (JavaScriptIntegrationUtil.isJsInjectionTag(context.name)) return
    val languageToInject = HtmlScriptLanguageInjector.getScriptLanguageToInject(context) ?: return
    if (!LanguageUtil.isInjectableLanguage(languageToInject)) return
    var started = false
    for (child in context.children) {
      if (child is GspOuterHtmlElement) {
        if (!started) {
          registrar.startInjecting(languageToInject)
          started = true
        }
        registrar.addPlace(
            null, null, child, TextRange.create(0, child.getTextLength())
        )
      }
    }
    if (started) registrar.doneInjecting()
  }

  override fun elementsToInjectIn(): List<Class<GspGrailsTag>> = listOf(GspGrailsTag::class.java)
}
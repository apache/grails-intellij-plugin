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
package org.apache.grails.intellij.plugin.pluginSupport.assetPipeline;

import com.intellij.patterns.XmlAttributeValuePattern;
import com.intellij.patterns.XmlNamedElementPattern;
import com.intellij.patterns.XmlPatterns;
import com.intellij.patterns.XmlTagPattern;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;

public final class AssetReferenceContributor extends PsiReferenceContributor {

  // #CHECK# asset.pipeline.grails.AssetsTagLib
  private static final String[][] ASSET_PLACES = {
    {"asset:javascript", "src"},
    {"asset:stylesheet", "href"},
    {"asset:stylesheet", "src"},
    {"asset:image", "src"},
    {"asset:link", "href"},
    {"asset:assetPathExists", "src"},
  };

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    AssetReferenceProvider provider = new AssetReferenceProvider();
    for (String[] place : ASSET_PLACES) {
      String tag = place[0];
      String attribute = place[1];
      XmlTagPattern.Capture tagPattern = XmlPatterns.xmlTag().withName(tag);
      XmlNamedElementPattern.XmlAttributePattern attributePattern =
        XmlPatterns.xmlAttribute(attribute).withParent(tagPattern);
      XmlAttributeValuePattern attributeValuePattern = XmlPatterns.xmlAttributeValue(attributePattern);
      registrar.registerReferenceProvider(attributeValuePattern, provider, PsiReferenceRegistrar.HIGHER_PRIORITY);
    }
  }
}

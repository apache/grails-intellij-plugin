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

package org.apache.grails.intellij.plugin;

import com.intellij.patterns.XmlPatterns;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.psi.filters.AndFilter;
import com.intellij.psi.filters.ClassFilter;
import com.intellij.psi.filters.OrFilter;
import com.intellij.psi.filters.ScopeFilter;
import com.intellij.psi.filters.TextFilter;
import com.intellij.psi.filters.position.NamespaceFilter;
import com.intellij.psi.filters.position.ParentElementFilter;
import com.intellij.xml.util.XmlUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.directive.GspDirective;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.directive.GspDirectiveAttributeValue;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.impl.gtag.reference.contributor.DefaultCodecDirectiveReferenceProvider;
import org.apache.grails.intellij.plugin.references.providers.GspImportListReferenceProvider;
import org.apache.grails.intellij.plugin.references.tagSupport.GspTagSupportGspReferenceProvider;

import static com.intellij.patterns.PlatformPatterns.psiElement;

public final class GrailsGspReferenceContributor extends PsiReferenceContributor {
  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {
    XmlUtil.registerXmlAttributeValueReferenceProvider(registrar,
                                                           new String[]{"import"},
                                                           new ScopeFilter(
                                                             new ParentElementFilter(
                                                               new AndFilter(
                                                                 new OrFilter(
                                                                   new AndFilter(
                                                                     new ClassFilter(GspDirective.class),
                                                                     new TextFilter("page")
                                                                   )
                                                                 ),
                                                                 new NamespaceFilter(XmlUtil.JSP_URI)
                                                               ),
                                                               2
                                                             )
                                                           ),
                                                           new GspImportListReferenceProvider()
        );

    registrar.registerReferenceProvider(psiElement(GspDirectiveAttributeValue.class)
                                          .withParent(XmlPatterns.xmlAttribute("defaultCodec")),
                                        new DefaultCodecDirectiveReferenceProvider());

    GspTagSupportGspReferenceProvider.register(registrar);
  }
}

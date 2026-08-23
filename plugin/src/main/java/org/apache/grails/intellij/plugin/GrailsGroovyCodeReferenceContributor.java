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

package org.apache.grails.intellij.plugin;

import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.pluginSupport.seachable.SearchableFieldReferenceProvider;
import org.apache.grails.intellij.plugin.references.GrailsMethodNamedArgumentReferenceProvider;
import org.apache.grails.intellij.plugin.references.buildConfig.BuildConfigFileReferenceProvider;
import org.apache.grails.intellij.plugin.references.controller.ControllerAllowedMethodReferenceProvider;
import org.apache.grails.intellij.plugin.references.controller.ControllerFieldReferenceProvider;
import org.apache.grails.intellij.plugin.references.controller.ControllerLayoutReferenceProvider;
import org.apache.grails.intellij.plugin.references.controller.ControllerReferenceProvider;
import org.apache.grails.intellij.plugin.references.domain.GormEmbeddedPropertyListReferenceProvider;
import org.apache.grails.intellij.plugin.references.domain.GormFetchModeReferenceProvider;
import org.apache.grails.intellij.plugin.references.domain.GormPropertiesListReferenceReferenceProvider;
import org.apache.grails.intellij.plugin.references.domain.GormPropertyConstraintReferenceProvider;
import org.apache.grails.intellij.plugin.references.domain.GormUniqueConstraintReferenceProvider;
import org.apache.grails.intellij.plugin.references.domain.GrailsHasManyBelongsToReferencesProvider;
import org.apache.grails.intellij.plugin.references.domain.GrailsHasManyBelongsToValuesReferencesProvider;
import org.apache.grails.intellij.plugin.references.filter.FilterReferenceProvider;
import org.apache.grails.intellij.plugin.references.pluginClass.GrailsPluginExcludeReferenceProvider;
import org.apache.grails.intellij.plugin.references.pluginClass.GrailsPluginListReferenceProvider;
import org.apache.grails.intellij.plugin.references.urlMappings.UrlMappingReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentList;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrIndexProperty;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.grField;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.namedArgumentStringLiteral;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.string;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.stringLiteral;

public final class GrailsGroovyCodeReferenceContributor extends PsiReferenceContributor {

  @Override
  public void registerReferenceProviders(@NotNull PsiReferenceRegistrar registrar) {

    registrar.registerReferenceProvider(stringLiteral().withParent(
      psiElement(GrListOrMap.class).withParent(grField().withName("transients").withModifiers(PsiModifier.STATIC))),
                                        new GormPropertiesListReferenceReferenceProvider());

    registrar.registerReferenceProvider(stringLiteral().withParent(
      psiElement(GrListOrMap.class).withParent(grField().withName("embedded").withModifiers(PsiModifier.STATIC))),
                                        new GormEmbeddedPropertyListReferenceProvider());

    registrar.registerReferenceProvider(namedArgumentStringLiteral(), new GrailsHasManyBelongsToValuesReferencesProvider());

    registrar.registerReferenceProvider(psiElement(GrArgumentLabel.class), new GrailsHasManyBelongsToReferencesProvider());

    registrar.registerReferenceProvider(stringLiteral(), new ControllerReferenceProvider());

    registrar.registerReferenceProvider(stringLiteral().withParent(grField().withModifiers(PsiModifier.STATIC)),
                                        new ControllerFieldReferenceProvider());

    registrar.registerReferenceProvider(namedArgumentStringLiteral(), new UrlMappingReferenceProvider());

    registrar.registerReferenceProvider(namedArgumentStringLiteral(), new FilterReferenceProvider());

    registrar.registerReferenceProvider(stringLiteral().withParent(psiElement(GrListOrMap.class).
      withParent(grField().withName("pluginExcludes"))), new GrailsPluginExcludeReferenceProvider());

    registrar.registerReferenceProvider(stringLiteral().withParent(psiElement(GrListOrMap.class).
      withParent(grField().withName(string().oneOf("observe", "influences", "loadAfter", "loadBefore")))),
                                        new GrailsPluginListReferenceProvider());

    registrar.registerReferenceProvider(stringLiteral().withParent(grField().withName("layout").withModifiers(PsiModifier.STATIC)),
                                        new ControllerLayoutReferenceProvider());

    registrar.registerReferenceProvider(stringLiteral().withParent(psiElement(GrArgumentList.class).withParent(GrIndexProperty.class)),
                                        new GormPropertyConstraintReferenceProvider());

    GormFetchModeReferenceProvider.register(registrar);

    registrar.registerReferenceProvider(stringLiteral(), new SearchableFieldReferenceProvider());

    BuildConfigFileReferenceProvider.register(registrar);

    registrar.registerReferenceProvider(stringLiteral(), GrailsMethodNamedArgumentReferenceProvider.getInstance());

    GormUniqueConstraintReferenceProvider.register(registrar);

    ControllerAllowedMethodReferenceProvider.register(registrar);
  }
}

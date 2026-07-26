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

package org.jetbrains.plugins.grails;

import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReferenceContributor;
import com.intellij.psi.PsiReferenceRegistrar;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.pluginSupport.seachable.SearchableFieldReferenceProvider;
import org.jetbrains.plugins.grails.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.grails.references.buildConfig.BuildConfigFileReferenceProvider;
import org.jetbrains.plugins.grails.references.controller.ControllerAllowedMethodReferenceProvider;
import org.jetbrains.plugins.grails.references.controller.ControllerFieldReferenceProvider;
import org.jetbrains.plugins.grails.references.controller.ControllerLayoutReferenceProvider;
import org.jetbrains.plugins.grails.references.controller.ControllerReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.GormEmbeddedPropertyListReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.GormFetchModeReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.GormPropertiesListReferenceReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.GormPropertyConstraintReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.GormUniqueConstraintReferenceProvider;
import org.jetbrains.plugins.grails.references.domain.GrailsHasManyBelongsToReferencesProvider;
import org.jetbrains.plugins.grails.references.domain.GrailsHasManyBelongsToValuesReferencesProvider;
import org.jetbrains.plugins.grails.references.filter.FilterReferenceProvider;
import org.jetbrains.plugins.grails.references.pluginClass.GrailsPluginExcludeReferenceProvider;
import org.jetbrains.plugins.grails.references.pluginClass.GrailsPluginListReferenceProvider;
import org.jetbrains.plugins.grails.references.urlMappings.UrlMappingReferenceProvider;
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

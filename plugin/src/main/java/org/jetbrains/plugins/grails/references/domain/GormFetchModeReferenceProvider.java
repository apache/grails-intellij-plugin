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

package org.jetbrains.plugins.grails.references.domain;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiReferenceRegistrar;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;

import static com.intellij.patterns.PlatformPatterns.psiElement;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.grField;
import static org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns.namedArgumentLabel;

public class GormFetchModeReferenceProvider extends PsiReferenceProvider {

  public static void register(PsiReferenceRegistrar registrar) {
    GormFetchModeReferenceProvider provider = new GormFetchModeReferenceProvider();

    registrar.registerReferenceProvider(
      namedArgumentLabel(null).withParent(psiElement(GrNamedArgument.class).withParent(psiElement(GrListOrMap.class).withParent(
          grField().withName("fetchMode").withModifiers(PsiModifier.STATIC))
      )),
      provider
    );
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    PsiElement namedArgument = element.getParent();
    PsiElement listOrMap = namedArgument.getParent();
    GrField field = (GrField)listOrMap.getParent();

    PsiClass domainClass = field.getContainingClass();
    if (domainClass == null) return PsiReference.EMPTY_ARRAY;
    if (!GormUtils.isGormBean(domainClass)) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{
      new GormPropertyReference(element, false, domainClass)
    };
  }
}

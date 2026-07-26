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

package org.apache.grails.intellij.plugin.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiType;
import com.intellij.psi.impl.light.LightElement;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.perspectives.graph.DomainClassRelationsInfo;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;

/**
 * @author Maxim.Medvedev
 */
public final class GrailsSyntheticFieldDeclarationSearcher extends PomDeclarationSearcher {
  @Override
  public void findDeclarationsAt(@NotNull PsiElement element, int offsetInElement, @NotNull Consumer<? super PomTarget> consumer) {
    if (!(element instanceof GrArgumentLabel)) return;

    PsiElement namedArgument = element.getParent();
    if (!(namedArgument instanceof GrNamedArgument)) return;

    final PsiElement context = namedArgument.getParent();
    if (!(context instanceof GrListOrMap)) return;

    final PsiElement parent = context.getParent();

    if (!(parent instanceof GrField)) return;

    String parentFiledName = ((GrField)parent).getName();

    if (!DomainClassRelationsInfo.MAPPED_BY.equals(parentFiledName) &&
        !DomainClassRelationsInfo.HAS_MANY_NAME.equals(parentFiledName) &&
        !DomainClassRelationsInfo.HAS_ONE_NAME.equals(parentFiledName) &&
        !DomainClassRelationsInfo.BELONGS_TO_NAME.equals(parentFiledName)) {
      return;
    }

    if (!((GrField)parent).hasModifierProperty(PsiModifier.STATIC)) return;

    final PsiClass psiClass = ((GrField)parent).getContainingClass();

    if (!GormUtils.isGormBean(psiClass)) return;
    assert psiClass != null;

    Pair<PsiType, PsiElement> pair = DomainDescriptor.getDescriptor(psiClass).getPersistentProperties()
      .get(((GrArgumentLabel)element).getName());

    if (pair == null) return;

    if (pair.second instanceof PomTarget && pair.second instanceof LightElement) {
      consumer.consume((PomTarget)pair.second);
    }
  }
}

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

package org.apache.grails.intellij.plugin.references.domain;

import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.perspectives.graph.DomainClassRelationsInfo;
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElementFactory;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrLiteral;

import java.util.Map;

/**
 * @author Maxim.Medvedev
 */
public class GrailsHasManyBelongsToValuesReferencesProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(final @NotNull PsiElement element, @NotNull ProcessingContext processingContext) {
    GrNamedArgument parent = (GrNamedArgument)element.getParent();
    PsiElement parent2 = parent.getParent();
    if (!(parent2 instanceof GrListOrMap)) return PsiReference.EMPTY_ARRAY;
    PsiElement parent3 = parent2.getParent();
    if (!(parent3 instanceof GrField)) return PsiReference.EMPTY_ARRAY;
    if (!DomainClassRelationsInfo.MAPPED_BY.equals(((GrField)parent3).getName())) return PsiReference.EMPTY_ARRAY;
    if (!((GrField)parent3).hasModifierProperty(PsiModifier.STATIC)) return PsiReference.EMPTY_ARRAY;

    final PsiClass psiClass = ((GrField)parent3).getContainingClass();
    if (!GormUtils.isGormBean(psiClass)) return PsiReference.EMPTY_ARRAY;
    assert psiClass != null;

    final Object value = ((GrLiteral)element).getValue();
    if (!(value instanceof String text)) return PsiReference.EMPTY_ARRAY;

    GrArgumentLabel label = parent.getLabel();
    if (label == null) return PsiReference.EMPTY_ARRAY;
    final String name = label.getName();
    if (name == null) return PsiReference.EMPTY_ARRAY;

    Pair<PsiType, PsiElement> p = DomainDescriptor.getDescriptor(psiClass).getHasMany().get(name);
    if (p == null) return PsiReference.EMPTY_ARRAY;

    PsiClass resolvedClass = PsiTypesUtil.getPsiClass(p.first);
    if (!GormUtils.isGormBean(resolvedClass)) return PsiReference.EMPTY_ARRAY;

    assert resolvedClass != null;

    final Map<String, Pair<PsiType, PsiElement>> map =
      DomainDescriptor.getDescriptor(resolvedClass).getPersistentProperties();
    final Pair<PsiType, PsiElement> pair = map.get(text);
    if (pair == null) return PsiReference.EMPTY_ARRAY;
    final PsiElement navigationElement = pair.second;

    return new PsiReference[]{new PsiReference() {
      @Override
      public @NotNull PsiElement getElement() {
        return element;
      }

      @Override
      public @NotNull TextRange getRangeInElement() {
        return new TextRange(0, element.getTextLength());
      }

      @Override
      public PsiElement resolve() {
        return navigationElement;
      }

      @Override
      public @NotNull String getCanonicalText() {
        return text;
      }

      @Override
      public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
        return element
          .replace(GroovyPsiElementFactory.getInstance(element.getProject()).createExpressionFromText("'" + newElementName + "'"));
      }

      @Override
      public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
        if (element instanceof GrField) {
          return element.replace(
            GroovyPsiElementFactory.getInstance(element.getProject()).createExpressionFromText("'" + ((GrField)element).getName() + "'"));
        }
        throw new IncorrectOperationException("Cannot bind element <" + element + "> to 'mappedBy' property");
      }

      @Override
      public boolean isReferenceTo(@NotNull PsiElement element) {
        return resolve() == element;
      }

      @Override
      public boolean isSoft() {
        return false;
      }
    }};
  }
}

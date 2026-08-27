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

package org.apache.grails.intellij.plugin.spring;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.spring.CommonSpringModel;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.converters.SpringConverterUtil;
import com.intellij.spring.model.utils.SpringModelUtils;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsCompletionContributor;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.modifiers.GrModifier;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrClassTypeElement;
import org.jetbrains.plugins.groovy.lang.psi.api.types.GrCodeReferenceElement;

import java.util.HashSet;
import java.util.Set;

public final class GrailsInjectedBeanCompletionContributor extends CompletionContributor {

  public GrailsInjectedBeanCompletionContributor() {
    extend(CompletionType.BASIC, GrailsCompletionContributor.grFieldNamePattern, new CompletionProvider<>() {
      @Override
      protected void addCompletions(@NotNull CompletionParameters parameters,
                                    @NotNull ProcessingContext context,
                                    @NotNull CompletionResultSet result) {

        PsiElement parent = parameters.getPosition().getParent();
        if (parent instanceof GrField) {
          if (((GrField)parent).getDeclaredType() != null) return;
        }
        else {
          assert parent instanceof GrCodeReferenceElement;
          PsiElement parent2 = parent.getParent();
          assert parent2 instanceof GrClassTypeElement;
          PsiElement variableDeclaration = parent2.getParent();
          assert variableDeclaration instanceof GrVariableDeclaration;
          if (!((GrVariableDeclaration)variableDeclaration).hasModifierProperty(GrModifier.DEF)) return;
        }

        PsiClass aClass = PsiTreeUtil.getParentOfType(parent, PsiClass.class);

        if (aClass == null) return;

        if (!InjectedSpringBeanProvider.isSupportInjection(aClass)) return;

        final CommonSpringModel springModel = SpringModelUtils.getInstance().getSpringModel(aClass);

        Set<SpringBeanPointer> beans = new HashSet<>(springModel.getAllCommonBeans());

        for (PsiField psiField : aClass.getFields()) {
          if (psiField instanceof GrField) {
            beans.remove(InjectedSpringBeanProvider.getInjectedBean(psiField));
          }
        }

        for (SpringBeanPointer beanPointer : beans) {
          LookupElement lookupElement = SpringConverterUtil.createCompletionVariant(beanPointer);
          if (lookupElement != null) {
            result.addElement(lookupElement);
          }
        }
      }
    });
  }
}

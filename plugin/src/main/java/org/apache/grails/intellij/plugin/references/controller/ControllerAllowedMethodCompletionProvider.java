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

package org.apache.grails.intellij.plugin.references.controller;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifier;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsPatterns;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.completion.handlers.NamedArgumentInsertHandler;
import org.jetbrains.plugins.groovy.lang.lexer.GroovyTokenTypes;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns;

import java.util.Set;

public final class ControllerAllowedMethodCompletionProvider extends CompletionContributor {
  private static final PsiElementPattern.Capture<GrListOrMap> LIST_OR_MAP_PATTERN = PlatformPatterns.psiElement(GrListOrMap.class).withParent(
    GroovyPatterns.grField().withName("allowedMethods").withModifiers(PsiModifier.STATIC)
      .inClass(GrailsPatterns.artifact(GrailsArtifact.CONTROLLER))
  );
  private static final ElementPattern<PsiElement> IN_LABEL =
    PlatformPatterns.psiElement(GroovyTokenTypes.mIDENT).withParent(PlatformPatterns.psiElement(GrArgumentLabel.class).withParent(
      PlatformPatterns.psiElement(GrNamedArgument.class).withParent(LIST_OR_MAP_PATTERN)));
  private static final ElementPattern<PsiElement> IN_NEW_LABEL =
    PlatformPatterns.psiElement(GroovyTokenTypes.mIDENT).withParent(PlatformPatterns.psiElement(GrReferenceExpression.class).withParent(
      LIST_OR_MAP_PATTERN));

  @Override
  public void fillCompletionVariants(@NotNull CompletionParameters parameters, @NotNull CompletionResultSet result) {
    if (parameters.getCompletionType() != CompletionType.BASIC && parameters.getCompletionType() != CompletionType.SMART) {
      return;
    }

    final PsiElement position = parameters.getPosition();
    if (!IN_LABEL.accepts(position) && !IN_NEW_LABEL.accepts(position)) {
      return;
    }

    PsiClass controller = PsiTreeUtil.getParentOfType(position, PsiClass.class);
    assert controller != null && GrailsArtifact.CONTROLLER.isInstance(controller);

    Module module = ModuleUtilCore.findModuleForPsiElement(controller);
    if (module == null) return;

    Set<String> actionNames = GrailsUtils.getControllerActions(GrailsArtifact.CONTROLLER.getArtifactName(controller), module).keySet();

    for (LookupElementBuilder lookupElement : ActionReference.createLookupItems(actionNames)) {
      result.addElement(PrioritizedLookupElement.withPriority(lookupElement.withInsertHandler(NamedArgumentInsertHandler.INSTANCE), 1));
    }
  }
}

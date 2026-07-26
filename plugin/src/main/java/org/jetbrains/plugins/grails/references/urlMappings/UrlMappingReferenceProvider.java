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

package org.jetbrains.plugins.grails.references.urlMappings;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.FunctionUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.common.GrailsViewFileReferenceSet;
import org.jetbrains.plugins.grails.references.controller.ActionReference;
import org.jetbrains.plugins.grails.references.controller.ControllerReference;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.util.PsiUtil;

public class UrlMappingReferenceProvider extends PsiReferenceProvider {
  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
    GrNamedArgument namedArgument = (GrNamedArgument)element.getParent();

    String key = namedArgument.getLabelName();

    if (!"action".equals(key) && !"controller".equals(key) && !"view".equals(key)) return PsiReference.EMPTY_ARRAY;

    GrMethodCall methodCall = PsiUtil.getMethodCallByNamedParameter(namedArgument);
    if (methodCall == null || !UrlMappingUtil.isMappingDefinition(methodCall)) return PsiReference.EMPTY_ARRAY;

    PsiReference ref;

    if ("action".equals(key)) {
      PsiElement controller = PsiUtil.getNamedArgumentValue(namedArgument, "controller");
      if (controller == null) return PsiReference.EMPTY_ARRAY;

      if (!(controller instanceof GrLiteralImpl)) return PsiReference.EMPTY_ARRAY;
      Object controllerValue = ((GrLiteralImpl)controller).getValue();
      if (!(controllerValue instanceof String)) return PsiReference.EMPTY_ARRAY;

      ref = new ActionReference(element, false, (String)controllerValue);
    }
    else if ("controller".equals(key)) {
      ref = new ControllerReference(element, false);
    }
    else {
      // assert "view".equals(key);
      return GrailsViewFileReferenceSet.createReferences(element, FunctionUtil.id());
    }

    return new PsiReference[]{ref};
  }
}

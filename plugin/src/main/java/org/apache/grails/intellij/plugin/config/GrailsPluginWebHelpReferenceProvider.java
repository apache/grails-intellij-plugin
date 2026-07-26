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

package org.apache.grails.intellij.plugin.config;

import com.intellij.openapi.paths.WebReference;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.util.ArrayUtilRt;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;

import java.util.regex.Matcher;

public class GrailsPluginWebHelpReferenceProvider extends GrailsMethodNamedArgumentReferenceProvider.Contributor.Provider
  implements GrailsMethodNamedArgumentReferenceProvider.Contributor {

  @Override
  public void register(GrailsMethodNamedArgumentReferenceProvider registrar) {
    registrar.register(0, this, new LightMethodCondition(GrailsPluginConfigMethodContributor.METHOD_KIND),
                       ArrayUtilRt.toStringArray(GrailsPluginConfigMethodContributor.SCOPES));
  }

  @Override
  public PsiReference[] createRef(final @NotNull PsiElement element,
                                  @NotNull GrMethodCall methodCall,
                                  int argumentIndex,
                                  @NotNull GroovyResolveResult resolveResult) {

    GrLiteralImpl literal = (GrLiteralImpl)element;

    String value = (String)literal.getValue();
    assert value != null;

    Matcher matcher = GrailsPluginNameCompletionContributor.DEPENDENCY_FORMAT.matcher(value);
    if (!matcher.matches()) return PsiReference.EMPTY_ARRAY;

    final String pluginName = matcher.group(2);

    if (pluginName.isEmpty()) return PsiReference.EMPTY_ARRAY;

    return new PsiReference[]{new WebReference(element, "https://grails.org/plugin/" + pluginName)};
  }
}

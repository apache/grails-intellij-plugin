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

package org.jetbrains.plugins.grails.references.tagSupport;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.resolve.taglib.TagLibNamespaceDescriptor;
import org.jetbrains.plugins.grails.references.GrailsMethodNamedArgumentReferenceProvider;
import org.jetbrains.plugins.grails.references.common.GroovyGspTagWrapper;
import org.jetbrains.plugins.grails.references.common.GspTagWrapper;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;

public abstract class TagAttributeReferenceProvider extends GrailsMethodNamedArgumentReferenceProvider.Contributor.Provider {

  private final String myAttributeName;
  private final String myNamespacePrefix;
  private final String[] myTagNames;

  protected TagAttributeReferenceProvider(@NotNull String attributeName, @Nullable String namespacePrefix, String @Nullable [] tagNames) {
    myAttributeName = attributeName;
    myNamespacePrefix = namespacePrefix;
    myTagNames = tagNames;
  }

  public String getAttributeName() {
    return myAttributeName;
  }

  public @Nullable String getPrefix() {
    return myNamespacePrefix;
  }

  public String @Nullable [] getTagNames() {
    return myTagNames;
  }

  public abstract PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                                  @NotNull String text,
                                                                  int offset,
                                                                  @NotNull GspTagWrapper gspTagWrapper);

  @Override
  public final PsiReference[] createRef(@NotNull PsiElement element,
                                        @NotNull GrNamedArgument namedArgument,
                                        @NotNull GroovyResolveResult resolveResult) {
    final TextRange range = ElementManipulators.getValueTextRange(element);
    int offset = range.getStartOffset();
    String text = range.substring(element.getText());

    TagLibNamespaceDescriptor.GspTagMethod tagMethod = (TagLibNamespaceDescriptor.GspTagMethod)resolveResult.getElement();
    assert tagMethod != null;

    if (myNamespacePrefix != null) {
      if (!myNamespacePrefix.equals(tagMethod.getNamespacePrefix())) {
        return PsiReference.EMPTY_ARRAY;
      }
    }

    return getReferencesByElement(element, text, offset, new GroovyGspTagWrapper((GrNamedArgumentsOwner)namedArgument.getParent(),
                                                                                 tagMethod));
  }
}

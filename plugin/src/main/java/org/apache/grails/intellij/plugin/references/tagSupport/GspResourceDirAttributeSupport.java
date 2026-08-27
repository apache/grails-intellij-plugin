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

package org.apache.grails.intellij.plugin.references.tagSupport;

import com.intellij.openapi.paths.PathReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.common.GroovyGspTagWrapper;
import org.apache.grails.intellij.plugin.references.common.GspTagWrapper;
import org.apache.grails.intellij.plugin.references.common.ResourceDirAttributeFileReferenceSet;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentReferenceProvider;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrNamedArgument;
import org.jetbrains.plugins.groovy.lang.psi.api.util.GrNamedArgumentsOwner;

public class GspResourceDirAttributeSupport extends TagAttributeReferenceProvider implements GroovyNamedArgumentReferenceProvider {

  public static final String[] TAGS = {"resource", "createLinkTo"};

  public GspResourceDirAttributeSupport() {
    super("dir", "g", TAGS);
  }

  @Override
  public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element,
                                                         @NotNull String text,
                                                         int offset,
                                                         @NotNull GspTagWrapper gspTagWrapper) {
    return createReferences(element, gspTagWrapper);
  }

  public static PsiReference[] createReferences(@NotNull PsiElement element, final @NotNull GspTagWrapper gspTagWrapper) {
    final TextRange range = ElementManipulators.getValueTextRange(element);
    int offset = range.getStartOffset();
    String text = range.substring(element.getText());

    String trimedUrl = PathReference.trimPath(text);

    if (trimedUrl.trim().isEmpty()) return PsiReference.EMPTY_ARRAY;

    final FileReferenceSet set = new ResourceDirAttributeFileReferenceSet(trimedUrl, element, offset, null, true, false) {

      @Override
      protected PsiElement getPluginElement() {
        return gspTagWrapper.getAttributeValue("plugin");
      }

      @Override
      protected PsiElement getContextPathElement() {
        return gspTagWrapper.getAttributeValue("contextPath");
      }
    };

    return set.getAllReferences();
  }

  @Override
  public PsiReference[] createRef(@NotNull PsiElement element,
                                  @NotNull GrNamedArgument namedArgument,
                                  @NotNull GroovyResolveResult resolveResult,
                                  @NotNull ProcessingContext context) {
    return createReferences(element, new GroovyGspTagWrapper((GrNamedArgumentsOwner)namedArgument.getParent(), null));
  }
}

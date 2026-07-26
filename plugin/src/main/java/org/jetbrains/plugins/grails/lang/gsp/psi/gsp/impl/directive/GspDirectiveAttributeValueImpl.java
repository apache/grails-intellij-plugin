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

package org.jetbrains.plugins.grails.lang.gsp.psi.gsp.impl.directive;

import com.intellij.codeInsight.daemon.impl.analysis.encoding.XmlEncodingReference;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceService;
import com.intellij.psi.impl.source.xml.XmlAttributeValueImpl;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspElementTypes;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttribute;
import org.jetbrains.plugins.grails.lang.gsp.psi.gsp.api.directive.GspDirectiveAttributeValue;
import org.jetbrains.plugins.grails.references.common.ContentTypeReference;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GspDirectiveAttributeValueImpl extends XmlAttributeValueImpl implements GspDirectiveAttributeValue {

  public static final Pattern CHARSET_PATTERN = Pattern.compile("\\bcharset=([a-z\\-0-9]*)", Pattern.CASE_INSENSITIVE);
  private static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile("\\s*([a-z\\-0-9/]+).*", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

  @Override
  public @NotNull IElementType getElementType() {
    return GspElementTypes.GSP_DIRECTIVE_ATTRIBUTE_VALUE;
  }

  @Override
  public PsiReference @NotNull [] getReferences(PsiReferenceService.@NotNull Hints hints) {
    PsiReference[] refs = super.getReferences(hints);
    if (refs.length > 0) return refs;

    GspDirectiveAttribute attribute = getContainingAttribute();
    if (attribute != null && "contentType".equals(attribute.getName())) {
      String value = getValue();

      List<PsiReference> result = new ArrayList<>();

      TextRange range = ElementManipulators.getValueTextRange(this);

      Matcher matcher = CHARSET_PATTERN.matcher(value);
      if (matcher.find()) {
        result.add(new XmlEncodingReference(this, matcher.group(1),
                                   TextRange.from(range.getStartOffset() + matcher.start(1), matcher.group(1).length()), 0));
      }

      matcher = CONTENT_TYPE_PATTERN.matcher(value);
      if (matcher.matches()) {
        result.add(new ContentTypeReference(this, TextRange.from(range.getStartOffset() + matcher.start(1), matcher.group(1).length()), true));
      }

      return result.toArray(PsiReference.EMPTY_ARRAY);
    }

    return PsiReference.EMPTY_ARRAY;
  }

  private @Nullable GspDirectiveAttribute getContainingAttribute() {
    PsiElement parent = getParent();
    if (parent instanceof GspDirectiveAttribute) {
      return (GspDirectiveAttribute) parent;
    }

    return null;
  }

  @Override
  public String toString() {
    return "GSP directive attribute value";
  }
}
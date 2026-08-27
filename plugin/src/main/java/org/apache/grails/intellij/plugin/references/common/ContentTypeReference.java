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

package org.apache.grails.intellij.plugin.references.common;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.xml.util.documentation.MimeTypeDictionary;
import org.jetbrains.annotations.NotNull;

public class ContentTypeReference extends PsiReferenceBase<PsiElement> {
  public ContentTypeReference(PsiElement element, TextRange range, boolean soft) {
    super(element, range, soft);
  }

  public ContentTypeReference(PsiElement element, TextRange range) {
    super(element, range);
  }

  public ContentTypeReference(PsiElement element, boolean soft) {
    super(element, soft);
  }

  public ContentTypeReference(@NotNull PsiElement element) {
    super(element);
  }

  @Override
  public PsiElement resolve() {
    return null;
  }

  @Override
  public Object @NotNull [] getVariants() {
    return MimeTypeDictionary.HTML_CONTENT_TYPES;
  }
}

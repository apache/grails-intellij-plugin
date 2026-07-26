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
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.references.util.PsiFieldReference;
import org.jetbrains.plugins.groovy.lang.completion.CompleteReferenceExpression;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GormPropertyReference extends PsiFieldReference {

  protected final PsiClass myDomainClass;

  public GormPropertyReference(PsiElement element, boolean soft, PsiClass domainClass) {
    super(element, soft);
    this.myDomainClass = domainClass;
  }

  @Override
  public PsiElement resolve() {
    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(myDomainClass);

    String propertyName = getValue();
    Pair<PsiType,PsiElement> pair = descriptor.getPersistentProperties().get(propertyName);
    return Pair.getSecond(pair);
  }

  @Override
  public Object @NotNull [] getVariants() {
    DomainDescriptor descriptor = DomainDescriptor.getDescriptor(myDomainClass);

    Map<String,Pair<PsiType,PsiElement>> map = descriptor.getPersistentProperties();

    List res = new ArrayList();

    for (Map.Entry<String, Pair<PsiType, PsiElement>> entry : map.entrySet()) {
      if (isValidForCompletion(entry.getKey(), entry.getValue().first, descriptor)) {
        res.add(CompleteReferenceExpression.createPropertyLookupElement(entry.getKey(), entry.getValue().first));
      }
    }

    return res.toArray();
  }

  protected boolean isValidForCompletion(String fieldName, PsiType type, DomainDescriptor descriptor) {
    return true;
  }

}

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

package org.apache.grails.intellij.plugin.references.domain.persistent;

import com.intellij.jpa.model.common.persistence.mapping.ElementCollection;
import com.intellij.jpa.model.xml.persistence.mapping.Enumerated;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.xml.GenericValue;
import com.intellij.util.xml.ReadOnlyGenericValue;

public class GormCollectionAttribute extends GormPersistentAttribute implements ElementCollection {
  public GormCollectionAttribute(GormEntity entity, String name, PsiType type, PsiElement element) {
    super(entity, name, type, element);
  }

  @Override
  public GenericValue<Enumerated> getEnumerated() {
    return ReadOnlyGenericValue.nullInstance();
  }

  @Override
  public GenericValue<PsiType> getComponentType() {
    return ReadOnlyGenericValue.getInstance(PsiUtil.extractIterableTypeParameter(myType, true));
  }

  @Override
  public boolean isContainer() {
    return true;
  }
}

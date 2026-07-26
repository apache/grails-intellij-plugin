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

package org.apache.grails.intellij.plugin.references.domain.persistent;

import com.intellij.jpa.model.common.persistence.mapping.ColumnBase;
import com.intellij.jpa.model.common.persistence.mapping.Id;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;

public class GormIdAttribute extends GormPersistentAttribute implements Id {
  public GormIdAttribute(GormEntity entity, String name, PsiType type, PsiElement element) {
    super(entity, name, type, element);
  }

  @Override
  public boolean isIdAttribute() {
    return true;
  }

  @Override
  public ColumnBase getColumn() {
    return null;
  }

}

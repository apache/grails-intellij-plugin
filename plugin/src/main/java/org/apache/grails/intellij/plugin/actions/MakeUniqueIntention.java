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

package org.apache.grails.intellij.plugin.actions;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.InheritanceUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;

public final class MakeUniqueIntention extends DomainFieldIntention {

  public MakeUniqueIntention() {
    super("unique", true);
  }

  @Override
  protected boolean isAppropriateField(@NotNull GrField field, @NotNull PsiType fieldType) {
    if (InheritanceUtil.isInheritor(fieldType, CommonClassNames.JAVA_UTIL_COLLECTION)) return false;
    if (!GormUtils.isGormBean(field.getContainingClass())) return false;
    return true;
  }

  @Override
  public @NotNull String getText() {
    return GrailsBundle.message("intention.text.make.property.unique");
  }

}

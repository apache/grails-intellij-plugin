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

package org.apache.grails.intellij.plugin.perspectives.graph;

import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class DomainClassNode {
  private final @NotNull PsiClass myTypeDefinition;

  public DomainClassNode(@NotNull PsiClass typeDefinition) {
    myTypeDefinition = typeDefinition;
  }

  public @NotNull String getUniqueName() {
    final String qualifiedName = myTypeDefinition.getQualifiedName();
    if (qualifiedName != null) {
      return qualifiedName;
    }

    return myTypeDefinition.getName();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    DomainClassNode that = (DomainClassNode) o;
    return Objects.equals(myTypeDefinition, that.myTypeDefinition);
  }

  @Override
  public int hashCode() {
    return myTypeDefinition.hashCode();
  }

  public @NotNull PsiClass getTypeDefinition() {
    return myTypeDefinition;
  }
}

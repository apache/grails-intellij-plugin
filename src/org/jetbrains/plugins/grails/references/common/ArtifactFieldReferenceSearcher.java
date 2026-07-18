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

package org.jetbrains.plugins.grails.references.common;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;

public final class ArtifactFieldReferenceSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

  public ArtifactFieldReferenceSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    PsiElement elementToSearch = queryParameters.getElementToSearch();

    if (!(elementToSearch instanceof GrField field)) return;

    if (!field.isProperty()) return;

    PsiMethod getter;

    PsiMethod[] getters = field.getGetters();
    if (getters.length != 1) return;

    getter = getters[0];

    ArtifactPropertyGetterReferenceSearcher
      .searchReferences(getter, elementToSearch, queryParameters.getEffectiveSearchScope(), queryParameters.getOptimizer());
  }
}

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

package org.apache.grails.intellij.plugin.lang.gsp.search;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.fileType.GspFileType;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

public final class GspControllerReferenceSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

  public GspControllerReferenceSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement elementToSearch = queryParameters.getElementToSearch();
    if (!(elementToSearch instanceof GrClassDefinition controller)) return;

    if (!GrailsArtifact.CONTROLLER.isInstance(controller)) return;

    final String text = GrailsArtifact.CONTROLLER.getArtifactName(controller);
    SearchScope searchScope = queryParameters.getEffectiveSearchScope();

    if (searchScope instanceof GlobalSearchScope) {
      searchScope = GlobalSearchScope.getScopeRestrictedByFileTypes(((GlobalSearchScope)searchScope), GspFileType.GSP_FILE_TYPE,
                                                                    GroovyFileType.GROOVY_FILE_TYPE);
    }
    queryParameters.getOptimizer()
      .searchWord(text, searchScope, (short)(UsageSearchContext.IN_FOREIGN_LANGUAGES | UsageSearchContext.IN_STRINGS), true, controller);
  }
}

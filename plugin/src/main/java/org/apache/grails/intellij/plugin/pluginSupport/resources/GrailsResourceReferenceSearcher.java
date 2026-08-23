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

package org.apache.grails.intellij.plugin.pluginSupport.resources;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.MethodReferencesSearch;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.fileType.GspFileType;
import org.jetbrains.plugins.groovy.GroovyFileType;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;

public final class GrailsResourceReferenceSearcher extends QueryExecutorBase<PsiReference, MethodReferencesSearch.SearchParameters> {

  public GrailsResourceReferenceSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull MethodReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    final PsiMethod elementToSearch = queryParameters.getMethod();
    if (!GrLightMethodBuilder.checkKind(elementToSearch, GrailsResourcesUtil.MODULE_METHOD_KIND)) return;
    
    GrMethodCall methodCall = (GrMethodCall)elementToSearch.getNavigationElement();
    GrReferenceExpression invokedExpression = (GrReferenceExpression)methodCall.getInvokedExpression();

    consumer.process(new ModuleDeclarationReference(invokedExpression, elementToSearch));
    
    SearchScope searchScope = queryParameters.getEffectiveSearchScope();

    if (searchScope instanceof GlobalSearchScope) {
      searchScope = GlobalSearchScope.getScopeRestrictedByFileTypes(((GlobalSearchScope)searchScope), GspFileType.GSP_FILE_TYPE,
                                                                    GroovyFileType.GROOVY_FILE_TYPE);
    }
    
    String text = elementToSearch.getName();
    
    queryParameters.getOptimizer().searchWord(text,
                                              searchScope,
                                              (short)(UsageSearchContext.IN_FOREIGN_LANGUAGES | UsageSearchContext.IN_STRINGS),
                                              true,
                                              elementToSearch);
  }

  private static class ModuleDeclarationReference extends PsiReferenceBase<PsiElement> {
    private final PsiMethod myModuleDeclaration;

    ModuleDeclarationReference(GrReferenceExpression invokedExpression, PsiMethod moduleDeclaration) {
      super(invokedExpression, new TextRange(0, invokedExpression.getTextLength()), false);
      myModuleDeclaration = moduleDeclaration;
    }

    @Override
    public PsiElement resolve() {
      return myModuleDeclaration;
    }

    @Override
    public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
      return ((GrReferenceExpression)getElement()).handleElementRename(newElementName);
    }
  }
}

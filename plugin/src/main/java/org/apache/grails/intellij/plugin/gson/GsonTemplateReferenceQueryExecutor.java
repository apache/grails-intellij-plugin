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
package org.apache.grails.intellij.plugin.gson;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.UsageSearchContext;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

public final class GsonTemplateReferenceQueryExecutor
  extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

  public GsonTemplateReferenceQueryExecutor() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters p,
                           @NotNull Processor<? super PsiReference> consumer) {
    if (!(p.getElementToSearch() instanceof GroovyFile element)) return;
    String templateName = GsonUtils.getGsonTemplateName(element);
    if (templateName == null) return;

    // Templates are referenced by bare name and by absolute path, so search for both spellings.
    p.getOptimizer().searchWord(templateName, p.getEffectiveSearchScope(), UsageSearchContext.IN_STRINGS, true, element);

    VirtualFile virtualFile = element.getVirtualFile();
    VirtualFile viewsRoot = ProjectFileIndex.getInstance(element.getProject()).getSourceRootForFile(virtualFile);
    if (viewsRoot == null) return;
    String path = VfsUtilCore.getRelativePath(virtualFile.getParent(), viewsRoot);
    if (path == null) return;
    String templateFqn = "/" + path + "/" + templateName;
    p.getOptimizer().searchWord(templateFqn, p.getEffectiveSearchScope(), UsageSearchContext.IN_STRINGS, true, element);
  }
}

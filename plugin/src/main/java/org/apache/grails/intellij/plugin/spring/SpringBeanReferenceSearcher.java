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

package org.apache.grails.intellij.plugin.spring;

import com.intellij.openapi.application.QueryExecutorBase;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiField;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.utils.SpringBeanUtils;
import com.intellij.util.Processor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.stubs.GroovyShortNamesCache;

public final class SpringBeanReferenceSearcher extends QueryExecutorBase<PsiReference, ReferencesSearch.SearchParameters> {

  public SpringBeanReferenceSearcher() {
    super(true);
  }

  @Override
  public void processQuery(@NotNull ReferencesSearch.SearchParameters queryParameters, @NotNull Processor<? super PsiReference> consumer) {
    final PsiElement element = queryParameters.getElementToSearch();

    SearchScope scope = queryParameters.getEffectiveSearchScope();
    if (!(scope instanceof GlobalSearchScope originalScope)) return;

    CommonSpringBean bean = SpringBeanUtils.getInstance().findBean(element);
    if (bean == null) return;

    String name = bean.getBeanName();
    if (name == null) return;

    Project project = element.getProject();

    GlobalSearchScope dirScope = null;

    for (Module module : ModuleManager.getInstance(project).getModules()) {
      if (originalScope.isSearchInModuleContent(module)) {
        VirtualFile appRoot = GrailsFramework.getInstance().findAppRoot(module);
        if (appRoot != null) {
          dirScope = unionWithDir(dirScope, project, appRoot.findChild(GrailsUtils.GRAILS_APP_DIRECTORY));
          dirScope = unionWithDir(dirScope, project, appRoot.findChild(GrailsUtils.TEST_DIR));
        }
      }
    }

    if (dirScope == null) return;

    for (final PsiField psiField : GroovyShortNamesCache.getGroovyShortNamesCache(project).getFieldsByName(name, originalScope.intersectWith(dirScope))) {
      SpringBeanPointer<?>  pointer = InjectedSpringBeanProvider.getInjectedBean(psiField);
      if (pointer != null && bean.equals(pointer.getSpringBean())) {
        ReferencesSearch.search(psiField).forEach(consumer);
      }
    }
  }

  private static @Nullable GlobalSearchScope unionWithDir(@Nullable GlobalSearchScope scope, Project project, @Nullable VirtualFile dir) {
    if (dir == null) return scope;

    GlobalSearchScope s = GlobalSearchScopesCore.directoryScope(project, dir, true);
    return scope == null ? s : scope.uniteWith(s);
  }
}

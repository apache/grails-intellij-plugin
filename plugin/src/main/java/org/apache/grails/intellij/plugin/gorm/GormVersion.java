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
package org.apache.grails.intellij.plugin.gorm;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.references.domain.DomainDescriptor;

public enum GormVersion {
  /**
   * GormApi compile time injection
   */
  BELOW_4,
  /**
   * GormEntity trait injection
   */
  IS_4,
  /**
   * GormEntity trait injection with custom traits for different DBs (Mongo, Cassandra, etc)
   */
  IS_5,
  /**
   * Custom traits for different DBs which are checked for availability in current context
   */
  IS_6;

  public static @Nullable GormVersion forElement(@Nullable PsiElement element) {
    return element == null ? null : forModule(ModuleUtilCore.findModuleForPsiElement(element));
  }

  public static @Nullable GormVersion forModule(@Nullable Module module) {
    if (module == null) return null;
    return CachedValuesManager.getManager(module.getProject()).getCachedValue(
      module,
      () -> Result.create(detect(module), ProjectRootManager.getInstance(module.getProject())));
  }

  private static @Nullable GormVersion detect(Module module) {
    JavaPsiFacade facade = JavaPsiFacade.getInstance(module.getProject());
    GlobalSearchScope scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module);

    // present since GORM 6
    if (facade.findClass(GormClassNames.QUERY_CREATOR, scope) != null) return IS_6;

    // present since GORM 5
    if (facade.findClass(GormClassNames.ENTITY_ANNO, scope) != null) return IS_5;

    // present since GORM 4
    if (facade.findClass(GormClassNames.ENTITY_TRAIT, scope) != null) return IS_4;

    // GormInstanceApi class is present in all versions, so we check it last
    if (facade.findClass(DomainDescriptor.GORM_INSTANCE_API_CLASS, scope) != null) return BELOW_4;

    return null;
  }
}

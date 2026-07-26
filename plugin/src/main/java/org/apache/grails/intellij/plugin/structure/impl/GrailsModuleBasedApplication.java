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
package org.apache.grails.intellij.plugin.structure.impl;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationBase;

public abstract class GrailsModuleBasedApplication extends GrailsApplicationBase {

  private final Module myModule;

  protected GrailsModuleBasedApplication(@NotNull Module module, @NotNull VirtualFile root) {
    super(module.getProject(), root);
    myModule = module;
  }

  public @NotNull Module getModule() {
    return myModule;
  }

  @Override
  public boolean isValid() {
    return super.isValid() && !myModule.isDisposed();
  }

  @Override
  public @NotNull GlobalSearchScope getScope(boolean includeDependencies, boolean testsOnly) {
    if (testsOnly && includeDependencies) {
      return myModule.getModuleTestsWithDependentsScope();
    }
    if (testsOnly) {
      // tests only, without dependencies: the test sources minus the production sources
      return CachedValuesManager.getManager(getProject()).getCachedValue(this, () -> {
        GlobalSearchScope moduleScope = myModule.getModuleScope(true);
        GlobalSearchScope moduleWithoutTestsScope = myModule.getModuleScope(false);
        GlobalSearchScope result = moduleScope.intersectWith(GlobalSearchScope.notScope(moduleWithoutTestsScope));
        return Result.create(result, ProjectRootManager.getInstance(getProject()));
      });
    }
    if (includeDependencies) {
      return myModule.getModuleRuntimeScope(false);
    }
    return myModule.getModuleScope(false);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || other.getClass() != getClass()) return false;
    if (!super.equals(other)) return false;
    return myModule.equals(((GrailsModuleBasedApplication)other).myModule);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + myModule.hashCode();
  }
}

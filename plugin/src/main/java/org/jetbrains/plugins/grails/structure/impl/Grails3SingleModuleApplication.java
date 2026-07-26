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
package org.jetbrains.plugins.grails.structure.impl;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;

final class Grails3SingleModuleApplication extends Grails3ApplicationBase {

  private final Module myModule;

  Grails3SingleModuleApplication(@NotNull Module module,
                                 @NotNull VirtualFile root,
                                 @NotNull DataNode<ModuleData> moduleDataNode) {
    super(module.getProject(), root, moduleDataNode);
    myModule = module;
  }

  public @NotNull Module getModule() {
    return myModule;
  }

  @Override
  public @NotNull GlobalSearchScope getScope(boolean includeDependencies, boolean testsOnly) {
    return includeDependencies ? myModule.getModuleRuntimeScope(testsOnly) : myModule.getModuleScope(testsOnly);
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || other.getClass() != getClass()) return false;
    if (!super.equals(other)) return false;
    return myModule.equals(((Grails3SingleModuleApplication)other).myModule);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + myModule.hashCode();
  }
}

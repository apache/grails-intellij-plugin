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

import com.intellij.lang.properties.psi.PropertiesFile;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsConfigUtils;
import org.jetbrains.plugins.grails.structure.OldGrailsApplication;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.grails.util.version.VersionImpl;

public abstract class OldGrailsModuleBasedApplication extends GrailsModuleBasedApplication implements OldGrailsApplication {

  private static final String[] TEST_FOLDERS = {"test/unit", "test/integration", "test/functional"};

  // Deferred and cached: computing it walks the VFS, and it is only needed for tests-only scopes.
  private volatile GlobalSearchScope myTestFoldersScope;

  protected OldGrailsModuleBasedApplication(@NotNull Module module, @NotNull VirtualFile root) {
    super(module, root);
  }

  private @NotNull GlobalSearchScope getTestFoldersScope() {
    GlobalSearchScope result = myTestFoldersScope;
    if (result == null) {
      result = GlobalSearchScope.EMPTY_SCOPE;
      for (String path : TEST_FOLDERS) {
        VirtualFile directory = getRoot().findFileByRelativePath(path);
        if (directory != null && directory.isDirectory()) {
          result = result.union(GlobalSearchScopesCore.directoryScope(getProject(), directory, true));
        }
      }
      myTestFoldersScope = result;
    }
    return result;
  }

  @Override
  public @NotNull Version getGrailsVersion() {
    String version = GrailsConfigUtils.getGrailsVersion(getModule());
    return version != null ? new VersionImpl(version) : Version.LATEST_2x;
  }

  @Override
  public @NotNull GlobalSearchScope getScope(boolean includeDependencies, boolean testsOnly) {
    GlobalSearchScope scope = super.getScope(includeDependencies, testsOnly);
    return testsOnly ? scope.union(getTestFoldersScope()) : scope;
  }

  @Override
  public @Nullable PropertiesFile getApplicationProperties() {
    VirtualFile root = getRoot();
    if (!root.isValid()) return null;

    VirtualFile applicationProperties = root.findChild("application.properties");
    if (applicationProperties == null) return null;

    PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(applicationProperties);
    return psiFile instanceof PropertiesFile propertiesFile ? propertiesFile : null;
  }
}

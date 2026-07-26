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
package org.jetbrains.plugins.grails.pluginSupport.assetPipeline;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiPackage;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReference;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceHelper;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceSet;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider.Result;
import com.intellij.psi.util.CachedValuesManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.plugins.Grails3SourcePluginDescriptor;
import org.jetbrains.plugins.grails.plugins.GrailsPlugins;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Resolves an asset-pipeline path against every {@code assets} folder reachable from the current
 * application: its own, those of source plugins, and the packaged ones inside plugin jars.
 */
public class AssetsFileReferenceSet extends FileReferenceSet {

  private static final String[] ASSETS_PACKAGES = {
    "META-INF.assets",
    "META-INF.static",
    "META-INF.resources",
  };

  public AssetsFileReferenceSet(@NotNull PsiElement element) {
    super(element);
  }

  @Override
  public @NotNull Collection<PsiFileSystemItem> computeDefaultContexts() {
    GrailsApplication application = GrailsApplicationManager.findApplication(getElement());
    return application == null ? Collections.emptyList() : getAssetFolders(application);
  }

  @Override
  public FileReference createFileReference(TextRange range, int index, String text) {
    return new FileReference(this, range, index, text) {
      @Override
      protected Collection<PsiFileSystemItem> getContextsForBindToElement(VirtualFile curVFile,
                                                                          Project project,
                                                                          FileReferenceHelper helper) {
        return getContexts();
      }
    };
  }

  @Override
  protected boolean isSoft() {
    return true;
  }

  private static @NotNull Collection<PsiFileSystemItem> getAssetFolders(@NotNull GrailsApplication application) {
    return CachedValuesManager.getManager(application.getProject()).getCachedValue(
      application,
      () -> Result.create(doGetAssetFolders(application), ProjectRootManager.getInstance(application.getProject())));
  }

  private static @NotNull Collection<PsiFileSystemItem> doGetAssetFolders(@NotNull GrailsApplication application) {
    Project project = application.getProject();

    List<VirtualFile> sourceFiles = new ArrayList<>();
    addAssetsFolderChildren(application, sourceFiles);
    for (Grails3SourcePluginDescriptor plugin : GrailsPlugins.getSourcePlugins(application)) {
      addAssetsFolderChildren(plugin.getPluginApplication(), sourceFiles);
    }

    PsiManager manager = PsiManager.getInstance(project);
    List<PsiFileSystemItem> result = new ArrayList<>();
    for (VirtualFile file : sourceFiles) {
      PsiDirectory directory = manager.findDirectory(file);
      if (directory != null) result.add(directory);
    }

    JavaPsiFacade facade = JavaPsiFacade.getInstance(project);
    GlobalSearchScope scope = application.getScope(true, false);
    for (String packageName : ASSETS_PACKAGES) {
      PsiPackage pckg = facade.findPackage(packageName);
      if (pckg == null) continue;
      Collections.addAll(result, pckg.getDirectories(scope));
    }

    return result;
  }

  private static void addAssetsFolderChildren(@NotNull GrailsApplication application, @NotNull List<VirtualFile> into) {
    VirtualFile assets = application.getAppRoot().findChild("assets");
    if (assets == null) return;
    VirtualFile[] children = assets.getChildren();
    if (children == null) return;
    Collections.addAll(into, children);
  }
}

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

package org.apache.grails.intellij.plugin;

import com.intellij.jarFinder.AbstractAttachSourceProvider;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.roots.LibraryOrderEntry;
import com.intellij.openapi.roots.OrderEntry;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.workspace.jps.entities.LibraryEntity;
import com.intellij.psi.PsiFile;
import com.intellij.util.ArrayUtil;
import com.intellij.workspaceModel.ide.legacyBridge.LibraryBridgesKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.config.GrailsConfigUtils;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;

final class GrailsAttachSourcesProvider extends AbstractAttachSourceProvider {

  private static final Logger LOG = Logger.getInstance(GrailsAttachSourcesProvider.class);

  @Override
  @Deprecated
  public @NotNull Collection<? extends AttachSourcesAction> getActions(@NotNull List<? extends LibraryOrderEntry> orderEntries,
                                                                       @NotNull PsiFile psiFile) {
    // Still abstract in the platform API; notifications use getLibrariesActions instead.
    return List.of();
  }

  @Override
  public @NotNull Collection<? extends AttachSourcesAction> getLibrariesActions(@NotNull Collection<LibraryEntity> libraryEntities,
                                                                               @NotNull PsiFile psiFile) {
    VirtualFile jar = getJarByPsiFile(psiFile);
    if (jar == null) return List.of();

    var libraries = new HashSet<>(libraryEntities);
    if (libraries.size() != 1) return List.of();

    final Library library = findLibrary(libraries.iterator().next(), psiFile);
    if (library == null) return List.of();

    VirtualFile[] files = library.getFiles(OrderRootType.CLASSES);

    VirtualFile grailsHome = GrailsConfigUtils.getGrailsLibraryHome(files);
    if (grailsHome == null) return List.of();

    final String grailsVersion = GrailsConfigUtils.getGrailsVersion(files);
    if (StringUtil.isEmpty(grailsVersion) || "1.4".compareTo(grailsVersion) > 0) return List.of();

    final String jarNameWithoutExt = jar.getNameWithoutExtension();

    if (!jarNameWithoutExt.endsWith(grailsVersion)) {
      LOG.warn("Grails JAR name don't end of version [jar:" + jar + ", version: " + grailsVersion + "]");
      return List.of();
    }

    String pluginNameWith_ = StringUtil.trimEnd(jarNameWithoutExt, grailsVersion);
    if (!pluginNameWith_.endsWith("-")) {
      LOG.warn("Grails JAR name don't end of version [jar:" + jar + ", version: " + grailsVersion + "]");
      return List.of();
    }

    final String pluginName = StringUtil.trimEnd(pluginNameWith_, "-").trim();

    final String sourceFileName = jarNameWithoutExt + "-sources.jar";

    // Discovery runs in a background read action, which forbids a synchronous VFS refresh, so the
    // directory is read as the VFS has it. A sources jar dropped into $GRAILS_HOME/src from outside
    // the IDE therefore shows up only once something else refreshes that directory.
    final VirtualFile grailsHomeSrc = grailsHome.findChild("src");
    if (grailsHomeSrc == null) {
      LOG.warn("Grails home don't contains 'scr' folder");
      return List.of();
    }

    VirtualFile srcFile = grailsHomeSrc.findChild(sourceFileName);
    if (srcFile != null) {
      // File already downloaded.
      VirtualFile jarRoot = JarFileSystem.getInstance().getJarRootForLocalFile(srcFile);
      if (jarRoot == null || ArrayUtil.contains(jarRoot, library.getFiles(OrderRootType.SOURCES))) {
        return List.of(); // Sources already attached.
      }

      return List.of(
        new AttachExistingSourceAction(
          jarRoot,
          library,
          GrailsBundle.message("attache.source.from.grails.action.title", "$GRAILS_HOME/src/"))
      );
    }

    final String url = "https://repo.grails.org/grails/libs-releases-local/org/grails/" + pluginName + '/' + grailsVersion + '/' + sourceFileName;

    return List.of(new DownloadSourcesAction(psiFile.getProject(), "Downloading Grails Sources", url) {
      @Override
      protected void storeFile(byte[] content) {
        try {
          VirtualFile srcFile = grailsHomeSrc.createChildData(this, sourceFileName);
          srcFile.setBinaryContent(content);

          addSourceFile(JarFileSystem.getInstance().getJarRootForLocalFile(srcFile), library);
        }
        catch (IOException e) {
          new Notification(myMessageGroupId,
                           GrailsBundle.message("notification.title.io.error"),
                           GrailsBundle.message("notification.content.failed.to.save.0.1", grailsHomeSrc.getPath(), sourceFileName),
                           NotificationType.ERROR)
            .notify(myProject);
          LOG.warn(e);
        }
      }
    });
  }

  /**
   * Resolves the entity to the {@link Library} the attach actions modify. The platform's direct
   * entity-to-library lookup ({@code findLibraryBridge}) is internal API, so the library is found among
   * the order entries that contain the file and matched by its workspace-model id.
   */
  private static @Nullable Library findLibrary(@NotNull LibraryEntity entity, @NotNull PsiFile psiFile) {
    VirtualFile file = psiFile.getVirtualFile();
    if (file == null) return null;

    for (OrderEntry entry : ProjectFileIndex.getInstance(psiFile.getProject()).getOrderEntriesForFile(file)) {
      if (entry instanceof LibraryOrderEntry libraryEntry) {
        Library library = libraryEntry.getLibrary();
        if (library != null && entity.getSymbolicId().equals(LibraryBridgesKt.findLibraryId(library))) {
          return library;
        }
      }
    }
    return null;
  }
}

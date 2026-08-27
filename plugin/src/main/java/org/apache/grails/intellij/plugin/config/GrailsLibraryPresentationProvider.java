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
package org.apache.grails.intellij.plugin.config;

import com.intellij.openapi.roots.JavadocOrderRootType;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.LibraryKind;
import com.intellij.openapi.roots.ui.configuration.libraryEditor.LibraryEditor;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.Nls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.jetbrains.plugins.groovy.config.GroovyLibraryPresentationProviderBase;
import org.jetbrains.plugins.groovy.config.GroovyLibraryProperties;

import javax.swing.Icon;
import java.io.File;
import java.util.Collection;

public final class GrailsLibraryPresentationProvider extends GroovyLibraryPresentationProviderBase {
  public static final LibraryKind GRAILS_KIND = LibraryKind.create("grails");

  private static final Collection<String> ROOT_FOLDER_NAMES =
    ContainerUtil.newHashSet("commons", "groovy", "persistence", "scaffolding", "tiger", "web", "java");

  public GrailsLibraryPresentationProvider() {
    super(GRAILS_KIND);
  }

  @Override
  public boolean managesLibrary(final VirtualFile[] libraryFiles) {
    return GrailsConfigUtils.getGrailsLibraryHome(libraryFiles) != null;
  }

  @Override
  public @Nls String getLibraryVersion(final VirtualFile[] libraryFiles) {
    return GrailsConfigUtils.getGrailsVersion(libraryFiles);
  }

  @Override
  public @NotNull Icon getIcon(GroovyLibraryProperties properties) {
    return GroovyMvcIcons.Grails_sdk;
  }

  @Override
  public boolean isSDKHome(@NotNull VirtualFile file) {
    return GrailsConfigUtils.getInstance().isSDKHome(file);
  }

  @Override
  public @Nullable String getSDKVersion(String path) {
    return GrailsConfigUtils.getInstance().getSDKVersionOrNull(path);
  }

  @Override
  protected void fillLibrary(String path, LibraryEditor libraryEditor) {
    File[] jars = new File(path + GrailsConfigUtils.DIST).listFiles();
    if (jars != null) {
      for (File file : jars) {
        String fileName = file.getName();

        if (fileName.endsWith(".jar")) {
          OrderRootType orderRootType;
          if (fileName.endsWith("-sources.jar")) {
            orderRootType = OrderRootType.SOURCES;
          }
          else if (fileName.endsWith("-javadoc.jar")) {
            orderRootType = JavadocOrderRootType.getInstance();
          }
          else {
            orderRootType = OrderRootType.CLASSES;
          }

          libraryEditor.addRoot(VfsUtil.getUrlForLibraryRoot(file), orderRootType);
        }
      }
    }

    collectJars(new File(path, "lib"), libraryEditor);

    File[] srcFiles = new File(path + "/src").listFiles();
    if (srcFiles != null) {
      for (File srcFile : srcFiles) {
        String name = srcFile.getName();
        if (srcFile.isDirectory() ? ROOT_FOLDER_NAMES.contains(name) : name.endsWith("-sources.jar")) {
          libraryEditor.addRoot(VfsUtil.getUrlForLibraryRoot(srcFile), OrderRootType.SOURCES);
        }
      }
    }

    File javadoc = new File(path + "/doc/api");
    if (javadoc.isDirectory()) {
      libraryEditor.addRoot(VfsUtil.getUrlForLibraryRoot(javadoc), JavadocOrderRootType.getInstance());
    }
  }

  private static void collectJars(File path, LibraryEditor libraryEditor) {
    File[] children = path.listFiles();
    if (children == null) return;

    for (File child : children) {
      if (child.isDirectory()) {
        collectJars(child, libraryEditor);
      }
      else if (child.getName().endsWith(".jar")) {
        if (child.getName().startsWith("javaee-web-api")) { // See issue #IDEA-78209 (https://youtrack.jetbrains.com/issue/IDEA-78209)
          continue;
        }

        OrderRootType orderRootType;

        if (child.getName().endsWith("-sources.jar")) {
          orderRootType = OrderRootType.SOURCES;
        }
        else if (child.getName().endsWith("-javadoc.jar")) {
          orderRootType = JavadocOrderRootType.getInstance();
        }
        else {
          orderRootType = OrderRootType.CLASSES;
        }

        libraryEditor.addRoot(VfsUtil.getUrlForLibraryRoot(child), orderRootType);
      }
    }
  }

  @Override
  public @Nls @NotNull String getLibraryCategoryName() {
    return GrailsBundle.message("library.name");
  }
}

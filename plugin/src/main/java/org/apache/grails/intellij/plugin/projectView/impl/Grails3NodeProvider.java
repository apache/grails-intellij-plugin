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

package org.apache.grails.intellij.plugin.projectView.impl;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileSystemItemFilter;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.projectView.GrailsPluginsNode;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;
import org.apache.grails.intellij.plugin.projectView.api.GrailsViewNodeProvider;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsPsiDirectoryNode;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.util.version.Version;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Grails3NodeProvider implements GrailsViewNodeProvider {

  private static final List<String> SPECIAL_FILES = List.of("build.gradle", "settings.gradle", "gradle.properties");
  private static final List<String> SPECIAL_DIRS = List.of("src/main/scripts", "src/main/webapp");

  /**
   * Test source roots that live under {@code src/}. Covers both the camelCase names used by the Grails
   * Gradle plugins up to 6.x and the kebab-case names introduced in Grails 7.
   */
  private static final Set<String> TEST_SOURCE_DIRS = Set.of(
    "test", "integrationTest", "integration-test", "functionalTest", "functional-test", "browserTest", "browser-test");

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> createNodes(@NotNull GrailsApplication application,
                                                              @NotNull ViewSettings settings) {
    if (application.getGrailsVersion().compareTo(Version.GRAILS_3_0) < 0) {
      return List.of();
    }

    Collection<AbstractTreeNode<?>> result = new ArrayList<>();

    List<PsiDirectory> specialDirs = new ArrayList<>();
    for (String path : SPECIAL_DIRS) {
      PsiDirectory directory = GrailsViewItems.findPsiDirectory(application, path);
      if (directory != null) specialDirs.add(directory);
    }
    for (PsiDirectory directory : specialDirs) {
      result.add(new GrailsPsiDirectoryNode(directory, settings, NodeWeights.SRC_FOLDERS));
    }

    PsiDirectory src = GrailsViewItems.findPsiDirectory(application, "src");
    if (src != null) {
      List<PsiDirectory> testDirs = findTestSourceDirectories(src);
      Set<PsiDirectory> hidden = new HashSet<>(testDirs);
      PsiFileSystemItemFilter filter =
        item -> !specialDirs.contains(item) && !hidden.contains(item) && GrailsViewItems.shouldShowItem(item);
      result.add(new GrailsPsiDirectoryNode(src, settings, NodeWeights.SRC_FOLDERS, filter));
      for (PsiDirectory testDir : testDirs) {
        boolean specialised = !"test".equals(testDir.getName());
        result.add(new GrailsPsiDirectoryNode(testDir, settings,
                                              specialised ? GroovyMvcIcons.Grails_test : PlatformIcons.TEST_SOURCE_FOLDER,
                                              NodeWeights.TESTS_FOLDER, null));
      }
    }

    for (String path : SPECIAL_FILES) {
      PsiFile file = GrailsViewItems.findPsiFile(application, path);
      if (file != null) {
        result.add(new PsiFileNode(application.getProject(), file, settings));
      }
    }

    result.add(new GrailsPluginsNode(application.getProject(), settings));
    return result;
  }

  /** Direct children of the {@code src} directory that look like test source roots. */
  private static @NotNull List<PsiDirectory> findTestSourceDirectories(@NotNull PsiDirectory src) {
    List<PsiDirectory> result = new ArrayList<>();
    VirtualFile[] children = src.getVirtualFile().getChildren();
    for (VirtualFile child : children) {
      if (child.isDirectory() && TEST_SOURCE_DIRS.contains(child.getName())) {
        PsiDirectory directory = PsiManager.getInstance(src.getProject()).findDirectory(child);
        if (directory != null) result.add(directory);
      }
    }
    return result;
  }
}

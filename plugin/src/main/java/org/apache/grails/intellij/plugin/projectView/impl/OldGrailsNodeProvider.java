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
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.javaee.JavaeeIcons;
import com.intellij.lang.Language;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.PlatformIcons;
import icons.JetgroovyIcons;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;
import org.apache.grails.intellij.plugin.projectView.api.GrailsViewNodeProvider;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsPsiDirectoryNode;
import org.apache.grails.intellij.plugin.projectView.nodes.OldGrailsPluginsNode;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.OldGrailsApplication;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class OldGrailsNodeProvider implements GrailsViewNodeProvider {

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> createNodes(@NotNull GrailsApplication application,
                                                              @NotNull ViewSettings settings) {
    if (!(application instanceof OldGrailsApplication oldApplication)) {
      return List.of();
    }

    Collection<AbstractTreeNode<?>> result = new ArrayList<>();
    Project project = oldApplication.getProject();
    PsiManager manager = PsiManager.getInstance(project);

    PsiFile applicationProperties = GrailsViewItems.findPsiFile(oldApplication, "application.properties");
    if (applicationProperties != null) {
      result.add(new PsiFileNode(project, applicationProperties, settings));
    }

    PsiDirectory webApp = GrailsViewItems.findPsiDirectory(oldApplication, "web-app");
    if (webApp != null) {
      result.add(new GrailsPsiDirectoryNode(webApp, settings, JavaeeIcons.WEB_FOLDER_CLOSED,
                                            NodeWeights.WEB_APP_FOLDER, "web-app"));
    }

    PsiDirectory scripts = GrailsViewItems.findPsiDirectory(oldApplication, "scripts");
    if (scripts != null) {
      result.add(new GrailsPsiDirectoryNode(scripts, settings, JetgroovyIcons.Groovy.Gant_16x16,
                                            NodeWeights.SRC_FOLDERS, "Scripts"));
    }

    for (VirtualFile child : oldApplication.getRoot().getChildren()) {
      if (child.getName().endsWith("GrailsPlugin.groovy")) {
        PsiFile pluginFile = manager.findFile(child);
        if (pluginFile != null) {
          result.add(new PsiFileNode(project, pluginFile, settings));
        }
        break;
      }
    }

    VirtualFile srcDir = oldApplication.getRoot().findChild("src");
    if (srcDir != null) {
      for (VirtualFile child : srcDir.getChildren()) {
        PsiDirectory directory = manager.findDirectory(child);
        if (directory == null) continue;
        String name = child.getName();
        result.add(new GrailsPsiDirectoryNode(directory, settings, sourceIcon(name), NodeWeights.SRC_FOLDERS,
                                              "Sources:" + name, GrailsViewItems::shouldShowItem));
      }
    }

    VirtualFile testDir = oldApplication.getRoot().findChild("test");
    if (testDir != null) {
      for (VirtualFile child : testDir.getChildren()) {
        PsiDirectory directory = manager.findDirectory(child);
        if (directory == null) continue;
        String name = child.getName();
        Icon icon = switch (name) {
          case "functional", "integration" -> GroovyMvcIcons.Grails_test;
          default -> PlatformIcons.TEST_SOURCE_FOLDER;
        };
        result.add(new GrailsPsiDirectoryNode(directory, settings, icon, NodeWeights.TESTS_FOLDER, "Tests:" + name));
      }
    }

    result.add(new OldGrailsPluginsNode(project, settings));
    return result;
  }

  /** Uses the language whose ID matches the directory name to pick an icon, as the Kotlin did. */
  private static @NotNull Icon sourceIcon(@NotNull String name) {
    Language language = Language.findLanguageByID(capitalize(name));
    if (language == null) {
      language = Language.findLanguageByID(name.toUpperCase(Locale.getDefault()));
    }
    if (language != null) {
      FileType fileType = language.getAssociatedFileType();
      if (fileType != null && fileType.getIcon() != null) {
        return fileType.getIcon();
      }
    }
    return PlatformIcons.SOURCE_FOLDERS_ICON;
  }

  private static @NotNull String capitalize(@NotNull String value) {
    if (value.isEmpty()) return value;
    return Character.toUpperCase(value.charAt(0)) + value.substring(1);
  }
}

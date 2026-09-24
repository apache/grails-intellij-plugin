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

import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiFileSystemItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.artefact.api.GrailsDisplayableArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.impl.GrailsArtefacts;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

import javax.swing.Icon;
import java.util.LinkedHashMap;
import java.util.Map;

public final class GrailsViewItems {

  /** Replaces the Kotlin Triple that carried the icon, weight and title of a special folder. */
  public record SpecialFolder(@NotNull Icon icon, int weight, @NotNull String title) {
  }

  public static final Map<String, SpecialFolder> SPECIAL_GRAILS_APP_FOLDERS = specialFolders();

  /** Subfolders of {@code grails-app/assets} shown as dedicated top-level nodes. */
  public static final Map<String, SpecialFolder> SPECIAL_ASSET_FOLDERS = assetFolders();

  /** Name of the {@code grails-app} directory whose subfolders are shown as dedicated nodes. */
  public static final String ASSETS_DIR = "assets";

  private GrailsViewItems() {
  }

  private static Map<String, SpecialFolder> specialFolders() {
    // Rendering order is decided by NodeWeights through GrailsNodeComparator; the LinkedHashMap
    // just keeps iteration stable for readability and for the contains()/dedup lookups.
    Map<String, SpecialFolder> result = new LinkedHashMap<>();
    result.put("conf", new SpecialFolder(AllIcons.Nodes.ConfigFolder, NodeWeights.CONFIG_FOLDER, "Configuration"));
    result.put("views", new SpecialFolder(GroovyMvcIcons.Gsp_logo, NodeWeights.VIEWS_FOLDER, "Views"));
    result.put("init", new SpecialFolder(AllIcons.Nodes.ConfigFolder, NodeWeights.CONFIG_FOLDER - 1, "Initialization"));
    result.put("i18n", new SpecialFolder(AllIcons.FileTypes.Properties, NodeWeights.TRANSLATIONS_FOLDER, "Translations"));
    return Map.copyOf(result);
  }

  private static Map<String, SpecialFolder> assetFolders() {
    Map<String, SpecialFolder> result = new LinkedHashMap<>();
    result.put(ASSETS_DIR + "/stylesheets", new SpecialFolder(AllIcons.FileTypes.Css, NodeWeights.STYLESHEETS_FOLDER, "Stylesheets"));
    result.put(ASSETS_DIR + "/images", new SpecialFolder(AllIcons.FileTypes.Image, NodeWeights.IMAGES_FOLDER, "Images"));
    result.put(ASSETS_DIR + "/javascripts", new SpecialFolder(AllIcons.FileTypes.JavaScript, NodeWeights.JAVASCRIPTS_FOLDER, "JavaScripts"));
    return Map.copyOf(result);
  }

  public static boolean shouldShowItem(@NotNull PsiFileSystemItem item) {
    if (!(item instanceof GroovyFile groovyFile)) return true;
    PsiClass[] classes = groovyFile.getClasses();
    if (classes.length != 1) return true;
    return !(GrailsArtefacts.getArtefactHandler(classes[0]) instanceof GrailsDisplayableArtefactHandler);
  }

  public static @Nullable PsiFile findPsiFile(@NotNull GrailsApplication application, @NotNull String name) {
    VirtualFile file = application.getRoot().findFileByRelativePath(name);
    return file != null ? PsiManager.getInstance(application.getProject()).findFile(file) : null;
  }

  public static @Nullable PsiDirectory findPsiDirectory(@NotNull GrailsApplication application, @NotNull String name) {
    VirtualFile file = application.getRoot().findFileByRelativePath(name);
    return file != null ? PsiManager.getInstance(application.getProject()).findDirectory(file) : null;
  }

  /** Resolves relative to {@code grails-app} rather than the application root. */
  public static @Nullable PsiDirectory findAppPsiDirectory(@NotNull GrailsApplication application, @NotNull String name) {
    VirtualFile file = application.getAppRoot().findFileByRelativePath(name);
    return file != null ? PsiManager.getInstance(application.getProject()).findDirectory(file) : null;
  }

  /** True if the given child directory name is one of the asset subfolders rendered as a dedicated node. */
  public static boolean isAssetSubfolder(@Nullable String name) {
    return name != null && SPECIAL_ASSET_FOLDERS.containsKey(ASSETS_DIR + "/" + name);
  }
}

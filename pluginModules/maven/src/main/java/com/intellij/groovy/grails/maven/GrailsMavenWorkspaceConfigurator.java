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
package com.intellij.groovy.grails.maven;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.idea.maven.importing.MavenStaticSyncAware;
import org.jetbrains.idea.maven.importing.MavenWorkspaceConfigurator;
import org.jetbrains.idea.maven.project.MavenProject;
import org.jetbrains.jps.model.module.JpsModuleSourceRootType;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.util.GrailsFacetProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Replaces the legacy {@link org.jetbrains.idea.maven.importing.MavenImporter}-based
 * GrailsMavenImporter, which is no longer invoked by the workspace-model Maven import
 * (2026.2): registers the Grails source/test/resource roots and the Grails facets for
 * projects built with org.grails:grails-maven-plugin.
 */
public final class GrailsMavenWorkspaceConfigurator implements MavenWorkspaceConfigurator, MavenStaticSyncAware {

  private static boolean isApplicable(@NotNull MavenProject mavenProject) {
    return mavenProject.findPlugin("org.grails", "grails-maven-plugin") != null;
  }

  @Override
  public @NotNull Stream<AdditionalFolder> getAdditionalFolders(@NotNull FoldersContext context) {
    if (!isApplicable(context.getMavenProject())) {
      return Stream.empty();
    }
    List<AdditionalFolder> folders = new ArrayList<>();
    for (JpsModuleSourceRootType<?> rootType : GrailsFramework.GRAILS_SOURCE_FOLDERS.keySet()) {
      FolderType folderType = toFolderType(rootType);
      for (String path : GrailsFramework.GRAILS_SOURCE_FOLDERS.get(rootType)) {
        folders.add(new AdditionalFolder(path, folderType));
      }
    }
    // the static preimport repairs default source dirs but not resources
    // (MavenProjectStaticImporter.resolveDirectories), so restore the Maven defaults here
    if (context.getMavenProject().getResources().isEmpty()) {
      folders.add(new AdditionalFolder("src/main/resources", FolderType.RESOURCE));
    }
    if (context.getMavenProject().getTestResources().isEmpty()) {
      folders.add(new AdditionalFolder("src/test/resources", FolderType.TEST_RESOURCE));
    }
    return folders.stream();
  }

  @Override
  public void afterModelApplied(@NotNull AppliedModelContext context) {
    // getMavenProjectsWithModules() is a kotlin.sequences.Sequence, which is not Iterable, so it
    // cannot drive a for-each from Java.
    var projects = context.getMavenProjectsWithModules().iterator();
    while (projects.hasNext()) {
      var projectWithModules = projects.next();
      MavenProject mavenProject = projectWithModules.getMavenProject();
      if (!isApplicable(mavenProject)) continue;

      for (var moduleWithType : projectWithModules.getModules()) {
        Module module = moduleWithType.getModule();
        List<Consumer<ModifiableFacetModel>> actions = new ArrayList<>();
        for (GrailsFacetProvider provider : GrailsFacetProvider.EP_NAME.getExtensions()) {
          provider.addFacets(actions, module, List.of(mavenProject.getDirectoryFile()));
        }
        if (actions.isEmpty()) continue;

        ApplicationManager.getApplication().invokeAndWait(
          () -> ApplicationManager.getApplication().runWriteAction(() -> {
            if (module.isDisposed()) return;
            ModifiableFacetModel facetModel = FacetManager.getInstance(module).createModifiableModel();
            for (Consumer<ModifiableFacetModel> action : actions) {
              action.consume(facetModel);
            }
            facetModel.commit();
          }));
      }
    }
  }
}

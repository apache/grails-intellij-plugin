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

package com.intellij.groovy.grails.hibernate;

import com.intellij.facet.FacetManager;
import com.intellij.facet.ModifiableFacetModel;
import com.intellij.hibernate.facet.HibernateFacet;
import com.intellij.hibernate.facet.HibernateFacetType;
import com.intellij.jpa.facet.JpaFacet;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationListener;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.structure.Grails3Application;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ensures modules that host a Grails 3+ application carry a Hibernate facet, so that the GORM
 * persistence bridge ({@code GormSessionFactoryContributor} -> {@code GormPersistenceMapping} ->
 * {@code GormEntity}) is activated and injected HQL can resolve domain classes and their properties
 * (navigation, find usages and rename).
 *
 * <p>For legacy Grails 2.x modules this facet is added through {@link GormHibernateFacetProvider}
 * during the MVC module-structure sync ({@code MvcProjectStructure.setupFacets}). That sync does not
 * run for Grails 3+ modules imported via Gradle, so the facet - and therefore the persistence model
 * exposing GORM entities - was never created there. This activity fills that gap without touching the
 * Gradle-managed source roots.
 */
final class Grails3HibernateFacetActivity implements StartupActivity.DumbAware {

  private static final String GORM_FACET_NAME = "Gorm";

  @Override
  public void runActivity(@NotNull Project project) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return;

    MessageBusConnection connection = project.getMessageBus().connect();
    connection.subscribe(GrailsApplicationListener.TOPIC, () -> ensureGormFacets(project));

    // Applications may already have been discovered (or recomputed by GrailsStartupActivity)
    // before this activity got a chance to subscribe above.
    ApplicationManager.getApplication().invokeLater(() -> ensureGormFacets(project), project.getDisposed());
  }

  private static void ensureGormFacets(@NotNull Project project) {
    if (project.isDisposed()) return;

    Set<Module> modules = ReadAction.compute(() -> {
      Set<Module> result = new LinkedHashSet<>();
      ProjectFileIndex fileIndex = ProjectFileIndex.getInstance(project);
      for (GrailsApplication application : GrailsApplicationManager.getInstance(project).getApplications()) {
        if (!(application instanceof Grails3Application) || !application.isValid()) continue;
        VirtualFile appRoot = application.getAppRoot();
        Module module = fileIndex.getModuleForFile(appRoot);
        if (module != null) result.add(module);
      }
      return result;
    });

    for (Module module : modules) {
      ensureGormFacet(module);
    }
  }

  private static void ensureGormFacet(@NotNull Module module) {
    if (module.isDisposed() || hasPersistenceFacet(module)) return;

    ApplicationManager.getApplication().invokeLater(() -> {
      if (module.isDisposed() || hasPersistenceFacet(module)) return;

      WriteAction.run(() -> {
        FacetManager facetManager = FacetManager.getInstance(module);
        ModifiableFacetModel model = facetManager.createModifiableModel();
        HibernateFacetType facetType = HibernateFacetType.getInstance();
        HibernateFacet facet = facetType.createFacet(module, GORM_FACET_NAME, facetType.createDefaultConfiguration(), null);
        model.addFacet(facet);
        model.commit();
      });
    }, module.getDisposed());
  }

  private static boolean hasPersistenceFacet(@NotNull Module module) {
    return ReadAction.compute(() -> {
      FacetManager facetManager = FacetManager.getInstance(module);
      return !facetManager.getFacetsByType(HibernateFacet.ID).isEmpty()
             || !facetManager.getFacetsByType(JpaFacet.ID).isEmpty();
    });
  }
}

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
package org.apache.grails.intellij.plugin.config;

import com.intellij.execution.ExecutionException;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.importing.ImportSpecBuilder;
import com.intellij.openapi.externalSystem.model.ExternalSystemDataKeys;
import com.intellij.openapi.externalSystem.model.project.ProjectData;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.service.project.wizard.AbstractGradleModuleBuilder;
import org.jetbrains.plugins.gradle.settings.DistributionType;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.runner.GrailsConsole;

import java.nio.file.Path;
import java.nio.file.Paths;

public abstract class GrailsGradleAwareModuleBuilder extends ModuleBuilder {

  protected static final Logger LOG = Logger.getInstance(GrailsGradleAwareModuleBuilder.class);

  private @Nullable ProjectData myParentProject;
  private boolean myIsCreatingNewProject;

  public @Nullable ProjectData getParentProject() {
    return myParentProject;
  }

  public void setParentProject(@Nullable ProjectData parentProject) {
    myParentProject = parentProject;
  }

  public boolean isCreatingNewProject() {
    return myIsCreatingNewProject;
  }

  public void setCreatingNewProject(boolean creatingNewProject) {
    myIsCreatingNewProject = creatingNewProject;
  }

  @Override
  protected void setupModule(@NotNull Module module) throws ConfigurationException {
    super.setupModule(module);
    if (myIsCreatingNewProject) {
      module.getProject().putUserData(ExternalSystemDataKeys.NEWLY_CREATED_PROJECT, Boolean.TRUE);
    }
  }

  protected void linkModule(@NotNull Module module) {
    Project project = module.getProject();
    VirtualFile root;
    try {
      root = getModuleRoot(module);
    }
    catch (ExecutionException e) {
      // linkModule is handed to GrailsModuleBuilder as a java.util.function.Consumer, so the
      // checked exception cannot be declared here. Nothing on that path catches ExecutionException,
      // so wrapping changes only the type that reaches the log.
      throw new RuntimeException(e);
    }

    Path rootProjectPath;
    String linkedPath = myParentProject == null ? null : myParentProject.getLinkedExternalProjectPath();
    if (linkedPath != null) {
      rootProjectPath = Paths.get(linkedPath);
    }
    else if (myIsCreatingNewProject) {
      rootProjectPath = Paths.get(module.getProject().getBasePath());
    }
    else {
      rootProjectPath = root.toNioPath();
    }

    boolean fresh = myIsCreatingNewProject || myParentProject == null;
    ApplicationManager.getApplication().runWriteAction(() -> {
      try {
        AbstractGradleModuleBuilder.setupGradleSettingsFile(rootProjectPath, root, project.getName(), module.getName(), fresh, false);
        LOG.debug("'settings.gradle' file set up for module: " + module);
      }
      catch (ConfigurationException e) {
        LOG.debug(e);
        GrailsConsole.NOTIFICATION_GROUP
          .createNotification(GrailsBundle.message("failed.to.create.settings.gradle.notification.title"),
                              e.getMessage() == null ? "" : e.getMessage(),
                              NotificationType.WARNING)
          .notify(project);
      }
    });

    linkGradleProject(fresh, rootProjectPath, project);
  }

  private void linkGradleProject(boolean fresh, @NotNull Path rootProjectPath, @NotNull Project project) {
    if (fresh) {
      GradleProjectSettings gradleSettings = new GradleProjectSettings();
      gradleSettings.setDistributionType(DistributionType.DEFAULT_WRAPPED);
      gradleSettings.setExternalProjectPath(invariantSeparatorsPathString(rootProjectPath));
      gradleSettings.setGradleJvm("#USE_PROJECT_JDK");

      ExternalSystemApiUtil.getSettings(project, GradleConstants.SYSTEM_ID).linkProject(gradleSettings);
      LOG.debug("Project '" + project + "' linked with " + gradleSettings);
    }

    if (!myIsCreatingNewProject) {
      FileDocumentManager.getInstance().saveAllDocuments();
      LOG.debug("Starting refreshing the project: " + rootProjectPath);
    }

    ExternalSystemUtil.refreshProject(invariantSeparatorsPathString(rootProjectPath),
                                     new ImportSpecBuilder(project, GradleConstants.SYSTEM_ID));
  }

  /**
   * Gradle wants forward slashes in linked project paths regardless of platform. Stands in for
   * Kotlin's {@code Path.invariantSeparatorsPathString}.
   */
  private static @NotNull String invariantSeparatorsPathString(@NotNull Path path) {
    String separator = path.getFileSystem().getSeparator();
    String asString = path.toString();
    return "/".equals(separator) ? asString : asString.replace(separator, "/");
  }

  protected @NotNull VirtualFile getModuleRoot(@NotNull Module module) throws ExecutionException {
    VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
    if (roots.length == 1) {
      return roots[0];
    }
    throw new ExecutionException(GrailsBundle.message("dialog.message.no.module.root"));
  }
}

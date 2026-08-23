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

package org.apache.grails.intellij.plugin.structure;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootEvent;
import com.intellij.openapi.roots.ModuleRootListener;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileEvent;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.openapi.vfs.impl.BulkVirtualFileListenerAdapter;
import com.intellij.util.messages.MessageBusConnection;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.settings.GradleProjectSettings;
import org.jetbrains.plugins.gradle.settings.GradleSettingsListener;
import org.apache.grails.intellij.plugin.config.GrailsConstants;
import org.apache.grails.intellij.plugin.config.GrailsFramework;

import java.util.Collection;
import java.util.Set;

/**
 * Registers listeners to queue the recalculation of Grails applications in {@link GrailsApplicationManager}.
 */
final class GrailsApplicationManagerRefresher implements ProjectActivity, DumbAware {
  @Override
  public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    if (ApplicationManager.getApplication().isUnitTestMode()) return null;

    final GrailsApplicationManager manager = GrailsApplicationManager.getInstance(project);
    final MessageBusConnection connection = project.getMessageBus().connect(manager);

    connection.subscribe(ModuleRootListener.TOPIC, new ModuleRootListener() {
      @Override
      public void rootsChanged(@NotNull ModuleRootEvent event) {
        Boolean inProgress = project.getUserData(GrailsFramework.UPDATE_IN_PROGRESS);
        if (inProgress != null && inProgress) return;
        manager.queueUpdate();
      }
    });

    connection.subscribe(VirtualFileManager.VFS_CHANGES, new BulkVirtualFileListenerAdapter(new VirtualFileListener() {

      final ProjectFileIndex myFileIndex = ProjectFileIndex.getInstance(project);

      boolean shouldClearApplications(@NotNull VirtualFileEvent event) {
        final VirtualFile file = event.getFile();
        if (!myFileIndex.isInContent(file)) return false;

        final String fileName = event.getFileName();
        return file.isDirectory() && fileName.equals(GrailsConstants.APP_DIRECTORY) ||
               !file.isDirectory() && fileName.equals(GrailsConstants.APPLICATION_PROPERTIES);
      }

      @Override
      public void fileCreated(@NotNull VirtualFileEvent event) {
        if (shouldClearApplications(event)) manager.queueUpdate();
      }

      @Override
      public void fileDeleted(@NotNull VirtualFileEvent event) {
        if (shouldClearApplications(event)) manager.queueUpdate();
      }
    }));

    connection.subscribe(GradleSettingsListener.TOPIC, new GradleSettingsListener() {
      @Override
      public void onProjectsLinked(@NotNull Collection<GradleProjectSettings> settings) {
        manager.queueUpdate();
      }

      @Override
      public void onProjectsUnlinked(@NotNull Set<String> linkedProjectPaths) {
        manager.queueUpdate();
      }
    });

    return null;
  }
}

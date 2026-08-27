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

package org.apache.grails.intellij.plugin.structure.sync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.ProjectActivity;
import com.intellij.util.messages.MessageBusConnection;
import kotlin.Unit;
import kotlin.coroutines.Continuation;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationListener;
import org.apache.grails.intellij.plugin.structure.GrailsSDKListener;
import org.apache.grails.intellij.plugin.structure.impl.Grails2Application;
import org.apache.grails.intellij.plugin.mvc.MvcModuleStructureSynchronizer;

final class GrailsMvcStructureRefresher implements ProjectActivity {
  @Override
  public @Nullable Object execute(@NotNull Project project, @NotNull Continuation<? super Unit> continuation) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return null;
    }

    final MvcModuleStructureSynchronizer synchronizer = MvcModuleStructureSynchronizer.getInstance(project);
    final MessageBusConnection connection = project.getMessageBus().connect();

    connection.subscribe(GrailsApplicationListener.TOPIC, () -> {
      synchronizer.getFileAndRootsModificationTracker().incModificationCount();
      ApplicationManager.getApplication().invokeLater(() -> {
        synchronizer.queue(MvcModuleStructureSynchronizer.SyncAction.UpdateProjectStructure, project);
        synchronizer.queue(MvcModuleStructureSynchronizer.SyncAction.UpgradeFramework, project);
      }, project.getDisposed());
    });

    connection.subscribe(GrailsSDKListener.TOPIC, application -> {
      if (!(application instanceof Grails2Application)) return;
      synchronizer.getFileAndRootsModificationTracker().incModificationCount();
      Module module = ((Grails2Application)application).getModule();
      ApplicationManager.getApplication().invokeLater(
        () -> GrailsFramework.forceSynchronizationSetting(module),
        module.getDisposed()
      );
    });

    return null;
  }
}

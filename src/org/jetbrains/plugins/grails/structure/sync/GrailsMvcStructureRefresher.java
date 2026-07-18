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

package org.jetbrains.plugins.grails.structure.sync;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.startup.StartupActivity;
import com.intellij.util.messages.MessageBusConnection;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.structure.GrailsApplicationListener;
import org.jetbrains.plugins.grails.structure.GrailsSDKListener;
import org.jetbrains.plugins.grails.structure.impl.Grails2Application;
import org.jetbrains.plugins.groovy.mvc.MvcModuleStructureSynchronizer;

final class GrailsMvcStructureRefresher implements StartupActivity {
  @Override
  public void runActivity(@NotNull Project project) {
    if (ApplicationManager.getApplication().isUnitTestMode()) {
      return;
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
  }
}

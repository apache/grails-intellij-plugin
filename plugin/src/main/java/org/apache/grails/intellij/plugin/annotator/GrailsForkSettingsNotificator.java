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

package org.apache.grails.intellij.plugin.annotator;

import com.intellij.openapi.fileEditor.FileEditor;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.ui.EditorNotificationPanel;
import com.intellij.ui.EditorNotificationProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.apache.grails.intellij.plugin.structure.OldGrailsApplication;
import org.apache.grails.intellij.plugin.structure.impl.Grails2Application;
import org.apache.grails.intellij.plugin.util.GrailsUtils;

import javax.swing.JComponent;
import java.util.function.Function;

// TODO find a better package to place this class
public final class GrailsForkSettingsNotificator implements EditorNotificationProvider, DumbAware {
  @Override
  public @Nullable Function<? super @NotNull FileEditor, ? extends @Nullable JComponent> collectNotificationData(@NotNull Project project,
                                                                                                                 @NotNull VirtualFile file) {
    if (!file.getName().equals(GrailsUtils.BUILD_CONFIG)) return null;
    final Module module = ModuleUtilCore.findModuleForFile(file, project);
    final OldGrailsApplication application = GrailsApplicationManager.findApplication(module);
    if (!(application instanceof Grails2Application)) return null;

    return fileEditor -> {
      final Grails2Application grails2Application = (Grails2Application)application;
      if ((grails2Application.isRunForked() || grails2Application.isTestForked()) &&
          !grails2Application.getGrailsVersion().isAtLeast("2.3.5")) {
        return createNotification(fileEditor, GrailsBundle.message("fork.mode.warning.2.3.5", grails2Application.getGrailsVersion()));
      }
      else if (grails2Application.isTestForked() && !grails2Application.getGrailsVersion().isAtLeast("2.3.10")) {
        return createNotification(fileEditor, GrailsBundle.message("fork.mode.warning.2.3.10", grails2Application.getGrailsVersion()));
      }
      return null;
    };
  }

  private static EditorNotificationPanel createNotification(@NotNull FileEditor fileEditor, @NlsContexts.LinkLabel String message) {
    final EditorNotificationPanel panel = new EditorNotificationPanel(fileEditor, EditorNotificationPanel.Status.Warning);
    panel.setText(message);
    return panel;
  }
}

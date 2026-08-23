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
package org.apache.grails.intellij.plugin.actions;

import com.intellij.execution.ExecutionException;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.runner.GrailsCommandExecutor;
import org.apache.grails.intellij.plugin.runner.GrailsConsole;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;

public class GrailsWarAction extends AnAction {
  private static final @NonNls String WAR_TARGET = "war";

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    GrailsApplication application = GrailsActionUtil.getGrailsApplication(e.getDataContext());
    GrailsCommandExecutor executor = GrailsCommandExecutor.getGrailsExecutor(application);
    if (executor == null) return;

    try {
      executor.execute(application, new MvcCommand(WAR_TARGET), null, true);
    }
    catch (ExecutionException ex) {
      GrailsConsole.NOTIFICATION_GROUP
        .createNotification(GrailsBundle.message("notification.title.failed.to.execute.grails.war"), ex.getMessage(), NotificationType.WARNING)
        .notify(e.getProject());
    }
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    GrailsApplication application = GrailsActionUtil.getGrailsApplication(e.getDataContext());
    e.getPresentation().setVisible(GrailsCommandExecutor.getGrailsExecutor(application) != null);
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }
}

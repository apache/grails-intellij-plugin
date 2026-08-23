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

package org.apache.grails.intellij.plugin.mvc.plugins.actions;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.runner.GrailsCommandExecutorUtil;
import org.apache.grails.intellij.plugin.runner.GrailsConsole;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;
import org.apache.grails.intellij.plugin.mvc.plugins.MvcPluginUtil;
import org.apache.grails.intellij.plugin.mvc.plugins.MvcPluginsMain;

public class ReloadMvcPluginListAction extends AnAction implements DumbAware {
  private final MvcPluginsMain myMvcPluginsMain;

  public ReloadMvcPluginListAction(final MvcPluginsMain mvcPluginsMain) {
    super(
      GrailsBundle.message("mvc.plugins.action.text.reload.list"),
      GrailsBundle.message("mvc.plugins.action.description.reload.list"),
      AllIcons.Actions.Refresh
    );
    myMvcPluginsMain = mvcPluginsMain;
  }

  @Override
  public void actionPerformed(final @NotNull AnActionEvent e) {
    Project project = myMvcPluginsMain.getProject();

    if (GrailsConsole.getInstance(project).isExecuting()) {
      Messages.showErrorDialog(project,
                               GrailsBundle.message("mvc.plugins.dialog.message.failed.to.reload.plugin.list"),
                               GrailsBundle.message("mvc.plugins.dialog.title.failed.to.execute.command")
      );
      return;
    }

    doReloadPluginList(myMvcPluginsMain);
  }

  public static void doReloadPluginList(final @NotNull MvcPluginsMain mvcPlugins) {
    assert !GrailsConsole.getInstance(mvcPlugins.getProject()).isExecuting();
    GrailsCommandExecutorUtil.executeInModal(
      mvcPlugins.getApplication(),
      new MvcCommand(MvcPluginUtil.LIST_PLUGINS_COMMAND),
      GrailsBundle.message("progress.text.updating.plugin.list"),
      mvcPlugins::reloadPlugins, true
    );
  }
}

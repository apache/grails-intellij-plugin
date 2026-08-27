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

package org.apache.grails.intellij.plugin.mvc.plugins;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.DialogBuilder;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.wm.StatusBar;
import com.intellij.openapi.wm.WindowManager;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.actions.GrailsActionUtil;
import org.apache.grails.intellij.plugin.runner.GrailsCommandExecutor;
import org.apache.grails.intellij.plugin.runner.GrailsConsole;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.OldGrailsApplication;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.apache.grails.intellij.plugin.mvc.plugins.actions.ReloadMvcPluginListAction;

import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class MvcPluginsAction extends AnAction {

  @Override
  public void update(@NotNull AnActionEvent e) {
    GrailsApplication application = GrailsActionUtil.getGrailsApplication(e.getDataContext());
    e.getPresentation().setEnabledAndVisible(
      application instanceof OldGrailsApplication &&
      application.getGrailsVersion().isLessThan(Version.GRAILS_2_3_0) &&
      GrailsCommandExecutor.getGrailsExecutor(application) != null
    );
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(@NotNull AnActionEvent e) {
    OldGrailsApplication application = (OldGrailsApplication)GrailsActionUtil.getGrailsApplication(e.getDataContext());
    assert application != null;
    final Project project = application.getProject();

    Runnable runnable = () -> {
      final DialogBuilder dialogBuilder = new DialogBuilder(project);

      dialogBuilder.addOkAction().setText(GrailsBundle.message("mvc.plugins.action.text.apply.changes"));

      final MvcPluginsMain mvcPluginsMain = new MvcPluginsMain(application, dialogBuilder);
      final AvailablePluginsModel tableModel = mvcPluginsMain.getPluginTable().getModel();

      final MvcPluginIsInstalledColumnInfo pluginIsInstalledColumnInfo =
        (MvcPluginIsInstalledColumnInfo)tableModel.getColumnInfos()[AvailablePluginsModel.COLUMN_IS_INSTALLED];

      dialogBuilder.setOkActionEnabled(false);
      dialogBuilder.addCancelAction();
      dialogBuilder.setTitle(GrailsBundle.message("mvc.plugins.dialog.title"));


      dialogBuilder.setCenterPanel(mvcPluginsMain.getMainPanel());

      dialogBuilder.setOkOperation(() -> {

        final Map<MvcPluginDescriptor, String> pluginToPath = mvcPluginsMain.getCustomPlugins();

        final Set<String> toRemovePluginNames = pluginIsInstalledColumnInfo.getToRemovePlugins();
        final Set<String> toInstallPluginNames = pluginIsInstalledColumnInfo.getToInstallPlugins();

        if (toRemovePluginNames.isEmpty() && toInstallPluginNames.isEmpty()) {
          final StatusBar statusBar = WindowManager.getInstance().getStatusBar(project);
          if (statusBar != null) {
            statusBar.setInfo(GrailsBundle.message("status.bar.text.no.plugins"));
          }
          return;
        }

        final Map<String, MvcPluginDescriptor> pluginMap = mvcPluginsMain.getPluginDescriptions();

        final List<MvcPluginDescriptor> toInstallCustomPlugins = new ArrayList<>();
        final List<MvcPluginDescriptor> toInstallServerPlugins = new ArrayList<>();

        for (String toInstallPluginName : toInstallPluginNames) {
          MvcPluginDescriptor plugin = pluginMap.get(toInstallPluginName);

          if (pluginToPath.containsKey(plugin)) {
            toInstallCustomPlugins.add(plugin);
          } else {
            toInstallServerPlugins.add(plugin);
          }
        }

        List<MvcPluginDescriptor> toRemovePlugins = new ArrayList<>(toRemovePluginNames.size());

        for (String toRemovePluginName : toRemovePluginNames) {
          toRemovePlugins.add(pluginMap.get(toRemovePluginName));
        }

        InstallUninstallPluginsDialog installUninstallPluginsDialog =
          new InstallUninstallPluginsDialog(toInstallServerPlugins, toInstallCustomPlugins, toRemovePlugins, application,
                                            mvcPluginsMain.getCustomPlugins());

        installUninstallPluginsDialog.show();
        if (DialogWrapper.OK_EXIT_CODE == installUninstallPluginsDialog.getExitCode()) {
          dialogBuilder.getDialogWrapper().close(DialogWrapper.OK_EXIT_CODE);
          installUninstallPluginsDialog.doInstallRemove();
        }
      });

      dialogBuilder.getWindow().addWindowListener(new WindowAdapter() {
        @Override
        public void windowOpened(WindowEvent e1) {
          Boolean f = MvcPluginUtil.PLUGIN_LIST_DONOT_DOWNLOADED.get(application);
          if (f != null && f) {
            MvcPluginUtil.PLUGIN_LIST_DONOT_DOWNLOADED.set(application, null);
            ApplicationManager.getApplication().invokeLater(() -> {
              if (dialogBuilder.getWindow().isShowing() && !GrailsConsole.getInstance(project).isExecuting()) {
                ReloadMvcPluginListAction.doReloadPluginList(mvcPluginsMain);
              }
            });
          }
        }
      });

      dialogBuilder.show();
    };

    GrailsConsole.getInstance(project).show(runnable, false);
  }
}

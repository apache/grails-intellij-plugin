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

package org.jetbrains.plugins.groovy.mvc.plugins.actions;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.fileChooser.FileChooser;
import com.intellij.openapi.fileChooser.FileChooserDescriptor;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.IconUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.GrailsBundle;
import org.jetbrains.plugins.groovy.mvc.plugins.MvcPluginDescriptor;
import org.jetbrains.plugins.groovy.mvc.plugins.MvcPluginUtil;
import org.jetbrains.plugins.groovy.mvc.plugins.MvcPluginsMain;

public class AddCustomPluginAction extends AnAction implements DumbAware {
  private final MvcPluginsMain myMvcPluginsMain;

  public AddCustomPluginAction(final MvcPluginsMain mvcPluginsMain) {
    super(GrailsBundle.message("mvc.plugins.action.text.add.custom.plugin"),
          GrailsBundle.message("mvc.plugins.action.description.add.custom.plugin"), IconUtil.getAddIcon());
    myMvcPluginsMain = mvcPluginsMain;
  }

  @Override
  public void actionPerformed(final @NotNull AnActionEvent e) {
    final FileChooserDescriptor descriptor = FileChooserDescriptorFactory.createSingleFileDescriptor("jar");
    final VirtualFile[] files = FileChooser.chooseFiles(descriptor, myMvcPluginsMain.getProject(), null);

    if (files.length > 0) {
      String pathToPlugin = files[0].getPath();

      MvcPluginDescriptor plugin = MvcPluginUtil.extractPluginInfo(pathToPlugin);
      if (plugin == null) {
        Messages.showErrorDialog(GrailsBundle.message("mvc.plugins.dialog.message.failed.to.read.plugin.archive"),
                                 GrailsBundle.message("mvc.plugins.dialog.title.failed.to.read.plugin.archive"));
        return;
      }

      myMvcPluginsMain.addCustomPlugin(plugin, pathToPlugin);
      myMvcPluginsMain.markInstalled(plugin.getName());

      myMvcPluginsMain.getFilter().setFilter("");
    }
  }
}

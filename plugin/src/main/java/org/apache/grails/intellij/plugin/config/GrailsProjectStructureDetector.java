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

import com.intellij.ide.util.PropertiesComponent;
import com.intellij.ide.util.importProject.ModuleDescriptor;
import com.intellij.ide.util.importProject.ProjectDescriptor;
import com.intellij.ide.util.projectWizard.ModuleBuilder;
import com.intellij.ide.util.projectWizard.ModuleWizardStep;
import com.intellij.ide.util.projectWizard.ProjectWizardStepFactory;
import com.intellij.ide.util.projectWizard.importSources.DetectedProjectRoot;
import com.intellij.ide.util.projectWizard.importSources.ProjectFromSourcesBuilder;
import com.intellij.ide.util.projectWizard.importSources.ProjectStructureDetector;
import com.intellij.openapi.module.JavaModuleType;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.sdk.GrailsSDKManager;
import org.apache.grails.intellij.plugin.ui.GrailsSDKHomeForm;
import org.apache.grails.intellij.plugin.util.version.Version;

import javax.swing.Icon;
import javax.swing.JComponent;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public final class GrailsProjectStructureDetector extends ProjectStructureDetector {

  @Override
  public @NotNull DirectoryProcessingResult detectRoots(@NotNull File dir,
                                                        File @NotNull [] children,
                                                        @NotNull File base,
                                                        @NotNull List<DetectedProjectRoot> result) {
    if (hasFilename(children, GrailsConstants.APP_DIRECTORY, false)
        && hasFilename(children, GrailsConstants.APPLICATION_PROPERTIES, true)
        && new File(new File(new File(dir, "grails-app"), "conf"), "BuildConfig.groovy").exists()) {
      result.add(new GrailsDetectedProjectRoot(dir));
      return DirectoryProcessingResult.PROCESS_CHILDREN;
    }
    return DirectoryProcessingResult.PROCESS_CHILDREN;
  }

  @Override
  public List<ModuleWizardStep> createWizardSteps(ProjectFromSourcesBuilder builder, ProjectDescriptor projectDescriptor, Icon stepIcon) {
    final ModuleWizardStep javaSdkStep = ProjectWizardStepFactory.getInstance().createProjectJdkStep(builder.getContext());
    final ModuleWizardStep groovySdkStep = new GrailsSDKWizardStep(builder, this);
    return Arrays.asList(javaSdkStep, groovySdkStep);
  }

  private static boolean hasFilename(File[] children, String name, boolean isFile) {
    for (File child : children) {
      if ((isFile ? child.isFile() : child.isDirectory()) && child.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }

  private static class GrailsDetectedProjectRoot extends DetectedProjectRoot {

    protected GrailsDetectedProjectRoot(@NotNull File directory) {
      super(directory);
    }

    @Override
    public @NotNull String getRootTypeName() {
      return GrailsBundle.message("library.name");
    }

    @Override
    public boolean canContainRoot(@NotNull DetectedProjectRoot root) {
      return GrailsDetectedProjectRoot.class.equals(root.getClass());
    }
  }

  private static class GrailsSDKWizardStep extends ModuleWizardStep {

    // allow selection of Grails < 3 sdks
    private final GrailsSDKHomeForm myForm = new GrailsSDKHomeForm().setVersionRange(Version.LESS_THAN_3);
    private final ProjectFromSourcesBuilder myFromSourcesBuilder;
    private final GrailsProjectStructureDetector myDetector;

    GrailsSDKWizardStep(ProjectFromSourcesBuilder builder, GrailsProjectStructureDetector detector) {
      myForm.setPath(PropertiesComponent.getInstance().getValue(GrailsConstants.GRAILS_LAST_SELECTED_SDK));
      myFromSourcesBuilder = builder;
      myDetector = detector;
    }

    @Override
    public JComponent getComponent() {
      return myForm.getComponent();
    }

    @Override
    public JComponent getPreferredFocusedComponent() {
      return myForm.getPathComponent();
    }

    @Override
    public boolean validate() throws ConfigurationException {
      return myForm.validate();
    }

    @Override
    public void updateDataModel() {
      final List<ModuleDescriptor> modules = ContainerUtil.map(myFromSourcesBuilder.getProjectRoots(myDetector), root -> {
        final File directory = root.getDirectory();
        final ModuleDescriptor descriptor = new ModuleDescriptor(directory, JavaModuleType.getModuleType(), Collections.emptyList());
        descriptor.addConfigurationUpdater(new ModuleBuilder.ModuleConfigurationUpdater() {
          @Override
          public void update(@NotNull Module module, @NotNull ModifiableRootModel rootModel) {
            // save Grails SDK path for later use
            GrailsSDKManager.getInstance(module.getProject()).setGrailsSDK(directory.getAbsolutePath(), myForm.getSelectedSdk().getPath());
          }
        });
        return descriptor;
      });
      myFromSourcesBuilder.getProjectDescriptor(myDetector).setModules(modules);
    }
  }
}
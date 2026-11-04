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

package org.apache.grails.intellij.plugin.editor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.actions.GrailsActionUtil;
import org.apache.grails.intellij.plugin.runner.GrailsCommandExecutorUtil;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.tests.GrailsTestUtils;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;

import java.util.Collection;
import java.util.List;

public class GenerateTestsAction extends AnAction {
  private final boolean myIntegration;

  private final String myArtifactName;

  private final GrailsArtifact myArtifactType;

  public GenerateTestsAction(boolean integration, @NotNull String artifactName, GrailsArtifact artifactType) {
    myIntegration = integration;
    myArtifactName = artifactName;
    myArtifactType = artifactType;
  }

  @Override
  public @NotNull ActionUpdateThread getActionUpdateThread() {
    return ActionUpdateThread.BGT;
  }

  @Override
  public void actionPerformed(final @NotNull AnActionEvent e) {
    final Module module = e.getData(PlatformCoreDataKeys.MODULE);
    if (module == null) return;

    GrailsApplication application = GrailsActionUtil.getGrailsApplication(e.getDataContext());
    if (application == null) return;

    final PsiClass testedClass = getTestedClass(module);

    if (testedClass == null) {
      switch (myArtifactType) {
        case DOMAIN -> Messages.showErrorDialog(GrailsBundle.message("generate.domain.class.first", myArtifactName),
                                                GrailsBundle.message("no.domain.class.found"));
        case CONTROLLER -> Messages.showErrorDialog(GrailsBundle.message("generate.controller.first", myArtifactName),
                                                    GrailsBundle.message("no.controller.found"));
        default -> {
          assert false;
        }
      }

      return;
    }

    final String qualifiedName = testedClass.getQualifiedName();
    if (qualifiedName == null) return;

    final MvcCommand mvcCommand = new MvcCommand(myIntegration ? "create-integration-test" : "create-unit-test", qualifiedName);
    GrailsCommandExecutorUtil.execute(application, mvcCommand, () -> {
      if (!testedClass.isValid()) return;
      // The console's own refresh after a command is asynchronous and not awaited, so the generated
      // spec is brought into the VFS explicitly before the index lookup below, which would
      // otherwise miss it: one scoped refresh per expected path, off the EDT.
      final List<String> expected = expectedTestPaths(application.getGrailsVersion(), qualifiedName, myIntegration);
      final VirtualFile root = application.getRoot();
      final String rootPath = root.getPath();
      final VirtualFileSystem fs = root.getFileSystem();
      ApplicationManager.getApplication().executeOnPooledThread(() -> {
        for (String relativePath : expected) {
          fs.refreshAndFindFileByPath(rootPath + '/' + relativePath);
        }
        ApplicationManager.getApplication().invokeLater(() -> {
          if (!testedClass.isValid()) return;
          PsiClass testClass = findTestClass(testedClass);
          if (testClass != null) {
            testClass.navigate(true);
          }
        });
      });
    }, true);
  }

  /**
   * The paths, relative to the application root, at which {@code create-unit-test} or
   * {@code create-integration-test} generates the test of the given artefact: the Grails 3+
   * layout since 3.0, and both the Spock and the older {@code Tests} name under the Grails 2
   * test directories before that.
   */
  static @NotNull List<String> expectedTestPaths(@NotNull Version grailsVersion, @NotNull String artefactFqn, boolean integration) {
    final String relative = artefactFqn.replace('.', '/');
    if (grailsVersion.isAtLeast(Version.GRAILS_3_0)) {
      return List.of((integration ? "src/integration-test/groovy/" : "src/test/groovy/") + relative + "Spec.groovy");
    }
    final String directory = integration ? GrailsUtils.GRAILS_INTEGRATION_TESTS : GrailsUtils.GRAILS_UNIT_TESTS;
    return List.of(directory + relative + "Spec.groovy", directory + relative + "Tests.groovy");
  }

  private @Nullable PsiClass findTestClass(PsiClass testedClass) {
    for (PsiClass aClass : GrailsTestUtils.getTestsForArtifact(testedClass, true)) {
      String testType = GrailsTestUtils.getTestType(aClass);
      if (myIntegration ? "integration".equals(testType) : "unit".equals(testType)) {
        return aClass;
      }
    }

    return null;
  }

  private @Nullable PsiClass getTestedClass(@NotNull Module module) {
    Collection<GrTypeDefinition> instances = myArtifactType.getInstances(module, myArtifactName);
    if (instances.isEmpty()) return null;

    return instances.iterator().next();
  }

  private boolean isEnabled(AnActionEvent e) {
    final Module module = e.getData(PlatformCoreDataKeys.MODULE);
    if (module == null || !GrailsUtils.hasSupport(module)) return false;

    PsiClass testedClass = getTestedClass(module);
    if (testedClass == null) return false;

    return findTestClass(testedClass) == null;
  }

  @Override
  public void update(@NotNull AnActionEvent e) {
    Presentation presentation = e.getPresentation();

    if (!presentation.isEnabled())
      return;

    if (!isEnabled(e)) {
      presentation.setEnabledAndVisible(false);
    }
  }

}
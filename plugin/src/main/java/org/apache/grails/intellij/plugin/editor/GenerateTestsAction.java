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

package org.apache.grails.intellij.plugin.editor;

import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.ui.Messages;
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
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;

import java.util.Collection;

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
      PsiClass testClass = findTestClass(testedClass);
      if (testClass != null) {
        testClass.navigate(true);
      }
    }, true);
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
    Collection<GrClassDefinition> instances = myArtifactType.getInstances(module, myArtifactName);
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
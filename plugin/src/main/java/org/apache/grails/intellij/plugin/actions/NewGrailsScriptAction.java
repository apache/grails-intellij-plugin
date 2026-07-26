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

package org.apache.grails.intellij.plugin.actions;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiNameHelper;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;

import java.util.List;

public class NewGrailsScriptAction extends NewGrailsXXXAction {

  public NewGrailsScriptAction() {
    super("action.Grails.NewScript.text");
  }

  @Override
  protected @NotNull String getCommand(@NotNull GrailsApplication application) {
    return "create-script";
  }

  @Override
  protected @Nullable VirtualFile getTargetDirectory(@NotNull GrailsApplication application) {
    return application.getAppRoot().findChild("scripts");
  }

  @Override
  protected void fillGeneratedNamesList(@NotNull String name, @NotNull List<String> names) {
    names.add("scripts/" + canonicalize(name) + ".groovy");
    names.add("test/cli/" + canonicalize(name) + "Tests.groovy");
  }

  @Override
  protected @NlsContexts.DialogMessage String isValidIdentifier(String inputString, Project project) {
    if (PsiNameHelper.getInstance(project).isIdentifier(inputString)) {
      return null;
    }
    return GrailsBundle.message("dialog.message.valid.script.name.check");
  }
}

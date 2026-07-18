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
package org.jetbrains.plugins.grails.perspectives;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.util.xml.ui.PerspectiveFileEditor;
import com.intellij.util.xml.ui.PerspectiveFileEditorProvider;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.references.domain.GormUtils;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

final class DomainClassesRelationsEditorProvider extends PerspectiveFileEditorProvider {
  @Override
  public boolean accept(@NotNull Project project, @NotNull VirtualFile file) {
    if (Registry.is("grails.advanced.mode")) {
      return false;
    }

    //noinspection SSBasedInspection
    return ApplicationManager.getApplication().runReadAction((Computable<Boolean>)() -> {
      PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
      if (!(psiFile instanceof GroovyFile)) {
        return false;
      }

      for (GrTypeDefinition grTypeDefinition : ((GroovyFile)psiFile).getTypeDefinitions()) {
        if (GormUtils.isGormBean(grTypeDefinition)) {
          return true;
        }
      }

      return false;
    });
  }

  @Override
  public boolean isDumbAware() {
    return false;
  }

  @Override
  public @NotNull PerspectiveFileEditor createEditor(@NotNull Project project, @NotNull VirtualFile virtualFile) {
    return new DomainClassesRelationsEditor(project, virtualFile);
  }
}

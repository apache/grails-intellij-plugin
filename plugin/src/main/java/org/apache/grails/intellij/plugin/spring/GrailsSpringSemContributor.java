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
package org.apache.grails.intellij.plugin.spring;

import com.intellij.java.library.JavaLibraryUtil;
import com.intellij.openapi.project.Project;
import com.intellij.patterns.PsiJavaPatterns;
import com.intellij.semantic.SemContributor;
import com.intellij.semantic.SemRegistrar;
import com.intellij.spring.model.jam.stereotype.SpringConfiguration;
import org.jetbrains.annotations.NotNull;

public class GrailsSpringSemContributor extends SemContributor {

  private static final String GRAILS_AUTO_CONFIGURATION = "grails.boot.config.GrailsAutoConfiguration";

  @Override
  protected boolean isAvailable(@NotNull Project project) {
    return JavaLibraryUtil.hasLibraryClass(project, GRAILS_AUTO_CONFIGURATION);
  }

  @Override
  public void registerSemProviders(@NotNull SemRegistrar registrar, @NotNull Project project) {
    registrar.registerSemElementProvider(
      SpringConfiguration.META_KEY,
      PsiJavaPatterns.psiClass().inheritorOf(true, GRAILS_AUTO_CONFIGURATION),
      psiClass -> SpringConfiguration.META
    );
  }
}

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

package org.jetbrains.plugins.grails.spring

import com.intellij.java.library.JavaLibraryUtil
import com.intellij.openapi.project.Project
import com.intellij.patterns.PsiJavaPatterns.psiClass
import com.intellij.semantic.SemContributor
import com.intellij.semantic.SemRegistrar
import com.intellij.spring.model.jam.stereotype.SpringConfiguration

class GrailsSpringSemContributor : SemContributor() {
  protected override fun isAvailable(project: Project): Boolean {
    return JavaLibraryUtil.hasLibraryClass(project, "grails.boot.config.GrailsAutoConfiguration")
  }

  override fun registerSemProviders(registrar: SemRegistrar, project: Project) {
    registrar.registerSemElementProvider(
      SpringConfiguration.META_KEY,
      psiClass().inheritorOf(true, "grails.boot.config.GrailsAutoConfiguration")
    ) { SpringConfiguration.META }
  }
}
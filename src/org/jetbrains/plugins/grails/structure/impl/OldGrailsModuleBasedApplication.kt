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

package org.jetbrains.plugins.grails.structure.impl

import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import org.jetbrains.plugins.grails.config.GrailsConfigUtils
import org.jetbrains.plugins.grails.structure.OldGrailsApplication
import org.jetbrains.plugins.grails.util.version.Version
import org.jetbrains.plugins.grails.util.version.Version.LATEST_2x
import org.jetbrains.plugins.grails.util.version.VersionImpl

abstract class OldGrailsModuleBasedApplication(
    module: Module,
    root: VirtualFile
) : GrailsModuleBasedApplication(module, root), OldGrailsApplication {

  private val myTestFoldersScope by lazy {
    arrayOf("test/unit", "test/integration", "test/functional").fold(GlobalSearchScope.EMPTY_SCOPE) { scope, path ->
      val directory = root.findFileByRelativePath(path)
      if (directory != null && directory.isDirectory) {
        scope.union(GlobalSearchScopesCore.directoryScope(project, directory, true))
      }
      else {
        scope
      }
    }
  }

  override val grailsVersion: Version get() = GrailsConfigUtils.getGrailsVersion(module)?.let { VersionImpl(it) } ?: LATEST_2x

  override fun getScope(includeDependencies: Boolean, testsOnly: Boolean): GlobalSearchScope {
    val scope = super.getScope(includeDependencies, testsOnly)
    if (testsOnly) {
      return scope.union(myTestFoldersScope)
    }
    else {
      return scope
    }
  }

  override val applicationProperties: PropertiesFile? get() {
    val root = root
    if (!root.isValid) return null

    val applicationProperties = root.findChild("application.properties") ?: return null

    val psiFile = PsiManager.getInstance(project).findFile(applicationProperties)
    return if (psiFile is PropertiesFile) psiFile else null
  }
}

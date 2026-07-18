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

package org.jetbrains.plugins.grails.config

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vcs.FilePath
import com.intellij.openapi.vcs.changes.IgnoredBeanFactory
import com.intellij.openapi.vcs.changes.IgnoredFileDescriptor
import com.intellij.openapi.vcs.changes.IgnoredFileProvider
import org.jetbrains.plugins.grails.GrailsBundle

internal class GrailsIgnoredProvider : IgnoredFileProvider {
  override fun isIgnoredFile(project: Project, filePath: FilePath) =
    FileUtil.isAncestor(GrailsFramework.getUserHomeGrails(), filePath.path, false)

  override fun getIgnoredFiles(project: Project): Set<IgnoredFileDescriptor> {
    val grailsDir = GrailsFramework.getUserHomeGrails()
    val projectBasePath = project.basePath ?: return emptySet()

    if (FileUtil.isAncestor(projectBasePath, grailsDir, true)) {
      return setOf(IgnoredBeanFactory.ignoreUnderDirectory(grailsDir, project))
    }

    return emptySet()
  }

  override fun getIgnoredGroupDescription() = GrailsBundle.message("ignored.files.description.framework.dir")
}
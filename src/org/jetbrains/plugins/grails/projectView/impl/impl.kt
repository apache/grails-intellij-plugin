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

package org.jetbrains.plugins.grails.projectView.impl

import com.intellij.icons.AllIcons
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.artefact.impl.getArtefactHandler
import org.jetbrains.plugins.grails.projectView.NodeWeights
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.grails.artefact.api.GrailsDisplayableArtefactHandler as ArtefactHandler

internal fun shouldShowItem(item: PsiFileSystemItem): Boolean {
  if (item !is GroovyFile) return true
  val clazz = item.classes.singleOrNull() ?: return true
  val handler = getArtefactHandler(clazz)
  return handler !is ArtefactHandler
}

internal val specialGrailsAppFolders = mapOf(
  "conf" to Triple(AllIcons.Nodes.ConfigFolder, NodeWeights.CONFIG_FOLDER, "Configuration"),
  "views" to Triple(GroovyMvcIcons.Gsp_logo, NodeWeights.VIEWS_FOLDER, "Views"),
  "init" to Triple(AllIcons.Nodes.ConfigFolder, NodeWeights.CONFIG_FOLDER - 1, "Initialization")
)

internal fun GrailsApplication.findPsiFile(name: String) = root.findFileByRelativePath(name)?.let {
  PsiManager.getInstance(project).findFile(it)
}

internal fun GrailsApplication.findPsiDirectory(name: String) = root.findFileByRelativePath(name)?.let {
  PsiManager.getInstance(project).findDirectory(it)
}

internal fun GrailsApplication.findAppPsiDirectory(name: String) = appRoot.findFileByRelativePath(name)?.let {
  PsiManager.getInstance(project).findDirectory(it)
}

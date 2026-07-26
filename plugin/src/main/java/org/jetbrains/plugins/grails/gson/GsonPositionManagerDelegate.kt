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

package org.jetbrains.plugins.grails.gson

import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import com.sun.jdi.ReferenceType
import org.jetbrains.plugins.grails.GsonConstants
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager
import org.jetbrains.plugins.groovy.extensions.debugger.ScriptPositionManagerHelper
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile

class GsonPositionManagerDelegate : ScriptPositionManagerHelper() {

  companion object {
    private const val suffix = "_${GsonConstants.EXTENSION}"
    private fun String.clean(): String = replace("[\\W\\s]".toRegex(), "_")
  }

  override fun isAppropriateScriptFile(scriptFile: GroovyFile): Boolean {
    return isGsonFile(scriptFile) && GrailsApplicationManager.findApplication(scriptFile) != null
  }

  /**
   * grails.views.resolve.GenericGroovyTemplateResolver#resolveTemplateName
   */
  override fun getRuntimeScriptName(groovyFile: GroovyFile): String? {
    val application = GrailsApplicationManager.findApplication(groovyFile) ?: return null
    val file = groovyFile.virtualFile ?: return null
    val viewsRoot = ProjectFileIndex.getInstance(groovyFile.project).getSourceRootForFile(file) ?: return null
    val path = VfsUtilCore.getRelativePath(file, viewsRoot) ?: return null
    return application.name.clean() + "_" + path.replace("/", "_").replace(".", "_")
  }

  override fun isAppropriateRuntimeName(runtimeName: String): Boolean = runtimeName.endsWith(suffix)

  override fun customizeClassName(psiClass: PsiClass): String? = (psiClass.containingFile as? GroovyFile)?.let {
    getRuntimeScriptName(it)
  }

  override fun getExtraScriptIfNotFound(refType: ReferenceType, runtimeName: String, project: Project, scope: GlobalSearchScope): PsiFile? {
    val appNameAndViewPath = runtimeName.removeSuffix(suffix)
    for (app in GrailsApplicationManager.getInstance(project).applications) {
      val viewPath = appNameAndViewPath.removePrefix(app.name.clean()).replace("_", "/")
      val viewFilePath = "views" + viewPath + GsonConstants.FILE_SUFFIX
      val view = app.appRoot.findFileByRelativePath(viewFilePath) ?: continue
      if (scope.contains(view)) PsiManager.getInstance(project).findFile(view)?.let { return it }
    }
    return null
  }
}

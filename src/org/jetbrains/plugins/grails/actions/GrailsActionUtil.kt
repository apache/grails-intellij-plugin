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

package org.jetbrains.plugins.grails.actions

import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.actionSystem.DataKey
import com.intellij.openapi.actionSystem.LangDataKeys
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler
import org.jetbrains.plugins.grails.structure.GrailsApplication
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager
import org.jetbrains.plugins.grails.tests.GrailsTestUtils
import org.jetbrains.plugins.grails.util.GrailsArtifact
import org.jetbrains.plugins.grails.util.GrailsUtils
import org.jetbrains.plugins.groovy.util.GroovyUtils

val GRAILS_APPLICATION: DataKey<GrailsApplication> = DataKey.create("grails.application")
val GRAILS_ARTEFACT_HANDLER: DataKey<GrailsArtefactHandler> = DataKey.create("grails.artefact.handler")
val GRAILS_ARTEFACT_PACKAGE: DataKey<String> = DataKey.create("grails.artefact.package")

class ArtefactData(
    val project: Project,
    val module: Module,
    val file: VirtualFile,
    val packageName: String?,
    val artefactName: String,
    val application: GrailsApplication,
    val isView: Boolean = false
)

private val LOG = com.intellij.openapi.diagnostic.logger<ArtefactData>()

fun getArtefactData(context: DataContext?): ArtefactData? {
  if (context == null) { LOG.warn("GRAILS-DBG getArtefactData: context is null"); return null }
  val project = context.getData(LangDataKeys.PROJECT) ?: run { LOG.warn("GRAILS-DBG getArtefactData: PROJECT null"); return null }
  if (DumbService.isDumb(project)) { LOG.warn("GRAILS-DBG getArtefactData: dumb mode"); return null }
  val module = context.getData(PlatformCoreDataKeys.MODULE) ?: run { LOG.warn("GRAILS-DBG getArtefactData: MODULE null"); return null }
  val file = context.getData(LangDataKeys.VIRTUAL_FILE) ?: run { LOG.warn("GRAILS-DBG getArtefactData: VIRTUAL_FILE null"); return null }
  val application = GrailsApplicationManager.getInstance(project).findApplication(file) ?: run { LOG.warn("GRAILS-DBG getArtefactData: findApplication null for ${file.name}"); return null }
  LOG.warn("GRAILS-DBG getArtefactData: passed gates for ${file.name}, module=${module.name}")
  val publicClass = GroovyUtils.getPublicClass(project, file)

  val isView: Boolean
  val packageName: String?
  val artefactName: String

  if (publicClass == null) {
    // inside a view
    val controllerName = GrailsUtils.getControllerNameByGsp(file) ?: return null
    if (controllerName == "layouts" || !StringUtil.isJavaIdentifier(controllerName)) return null
    isView = true
    packageName = null // we do not know package here
    artefactName = controllerName
  }
  else {
    val artefactClass = if (GrailsUtils.isInGrailsTests(file, project)) {
      GrailsTestUtils.getTestedClass(publicClass)
    }
    else {
      publicClass
    }
    artefactClass ?: run { LOG.warn("GRAILS-DBG getArtefactData: artefactClass null"); return null }
    isView = false
    packageName = StringUtil.getPackageName(artefactClass.qualifiedName ?: run { LOG.warn("GRAILS-DBG getArtefactData: qualifiedName null"); return null })
    artefactName = GrailsArtifact.getType(artefactClass)?.getArtifactName(artefactClass) ?: run { LOG.warn("GRAILS-DBG getArtefactData: artifact type/name null for ${artefactClass.qualifiedName}"); return null }
  }
  LOG.warn("GRAILS-DBG getArtefactData: SUCCESS name=$artefactName isView=$isView")

  return ArtefactData(project, module, file, packageName, artefactName, application, isView)
}

fun getGrailsApplication(dataContext: DataContext): GrailsApplication? {
  dataContext.getData(GRAILS_APPLICATION)?.let { return it }
  val project = CommonDataKeys.PROJECT.getData(dataContext) ?: return null
  val instance = GrailsApplicationManager.getInstance(project)
  val virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext) ?: return null
  return instance.findApplication(virtualFile)
}

fun getArtefactHandler(dataContext: DataContext): GrailsArtefactHandler? = dataContext.getData(GRAILS_ARTEFACT_HANDLER)

fun getArtefactPackage(dataContext: DataContext): String? = dataContext.getData(GRAILS_ARTEFACT_PACKAGE)
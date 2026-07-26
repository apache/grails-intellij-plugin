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

package org.jetbrains.plugins.grails.editor.toolbar

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.vfs.LocalFileSystem
import org.jetbrains.plugins.grails.GrailsBundle
import org.jetbrains.plugins.grails.GroovyMvcIcons
import org.jetbrains.plugins.grails.actions.ArtefactData
import org.jetbrains.plugins.grails.actions.getArtefactData
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutor
import org.jetbrains.plugins.grails.runner.GrailsCommandExecutorUtil
import org.jetbrains.plugins.grails.util.GrailsArtifact
import org.jetbrains.plugins.groovy.mvc.MvcCommand
import javax.swing.Icon

abstract class GenerateActionBase(val command: String, @ActionText text: String?, icon: Icon?) : AnAction(text, null, icon) {

  override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

  override fun update(e: AnActionEvent) {
    e.presentation.isEnabledAndVisible = isEnabled(e.dataContext)
  }

  override fun actionPerformed(e: AnActionEvent) {
    val data = getArtefactData(e.dataContext) ?: return
    val domainClass = GrailsArtifact.DOMAIN.getInstances(data.module, data.packageName, data.artefactName).singleOrNull() ?: return
    val domainClassName = domainClass.qualifiedName ?: return

    GrailsCommandExecutorUtil.execute(data.application, MvcCommand(command, domainClassName)) {
      LocalFileSystem.getInstance().refreshFiles(listOf(data.application.root), true, true) {
        onDone(data)
      }
    }
  }

  open fun isEnabled(dataContext: DataContext): Boolean {
    val data = getArtefactData(dataContext) ?: return false
    val domainClass = GrailsArtifact.DOMAIN.getInstances(data.module, data.packageName, data.artefactName).singleOrNull()
    return domainClass != null && isEnabled(data)
  }

  open fun isEnabled(data: ArtefactData): Boolean = GrailsCommandExecutor.getGrailsExecutor(data.application) != null

  open fun onDone(data: ArtefactData): Unit = Unit
}

abstract class GenerateControllerActionBase(command: String, @ActionText text: String?)
  : GenerateActionBase(command, text, AllIcons.Nodes.Controller) {

  override fun isEnabled(data: ArtefactData): Boolean = GrailsArtifact.CONTROLLER.getInstances(
    data.module, data.packageName, data.artefactName
  ).singleOrNull() == null

  override fun onDone(data: ArtefactData) {
    GrailsArtifact.CONTROLLER.getInstances(data.module, data.packageName, data.artefactName).singleOrNull()?.navigate(true)
  }
}

class GenerateControllerAction : GenerateControllerActionBase("generate-controller",
                                                              GrailsBundle.message("action.text.generate.controller"))

class GenerateAsyncControllerAction : GenerateControllerActionBase("generate-async-controller",
                                                                   GrailsBundle.message("action.text.generate.async.controller"))

class GenerateViewsAction : GenerateActionBase("generate-views",
                                               GrailsBundle.message("action.text.generate.views"),
                                               GroovyMvcIcons.Gsp_logo)
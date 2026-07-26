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

package org.jetbrains.plugins.grails.editor

import com.intellij.ide.actions.DistractionFreeModeController
import com.intellij.ide.ui.UISettings
import com.intellij.ide.util.propComponentProperty
import com.intellij.openapi.actionSystem.ActionGroup
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.text.StringUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.EditorNotificationProvider
import org.jetbrains.plugins.grails.GsonConstants
import org.jetbrains.plugins.grails.fileType.GspFileType
import org.jetbrains.plugins.grails.util.GrailsArtifact
import org.jetbrains.plugins.grails.util.GrailsUtils
import org.jetbrains.plugins.groovy.GroovyFileType
import org.jetbrains.plugins.groovy.util.GroovyUtils
import java.util.function.Function
import javax.swing.JComponent

internal class GrailsEditorDecorator : EditorNotificationProvider {
  override fun collectNotificationData(project: Project, file: VirtualFile): Function<in FileEditor, out JComponent?>? {
    if (!showEditorToolBar) return null
    if (!shouldBeDecorated(project, file)) {
      return null
    }
    return Function(::createNotificationPanel)
  }

  private fun createNotificationPanel(fileEditor: FileEditor): JComponent? {
    if (!shouldBeDecorated(fileEditor)) {
      return null
    }
    val manager = ActionManager.getInstance()
    val group = manager.getAction("grails.toolbar") as ActionGroup
    val toolbar = manager.createActionToolbar(ActionPlaces.EDITOR_TOOLBAR, group, true)
    toolbar.setTargetComponent(fileEditor.component)
    return toolbar.component
  }
}

var showEditorToolBar: Boolean by propComponentProperty(name = "grails.show.editor.toolbar",
                                                        defaultValue = System.getProperty("grails.show.editor.toolbar", "true").toBooleanStrict())

private fun shouldBeDecorated(fileEditor: FileEditor): Boolean {
  return fileEditor is TextEditor
         && !UISettings.getInstance().presentationMode
         && !DistractionFreeModeController.isDistractionFreeModeEnabled()
}

fun shouldBeDecorated(file: VirtualFile, fileEditor: FileEditor, project: Project): Boolean {
  return shouldBeDecorated(fileEditor)
         && shouldBeDecorated(project, file)
}

val DECORATED_ARTEFACT_TYPES: Array<GrailsArtifact> = arrayOf(
  GrailsArtifact.DOMAIN,
  GrailsArtifact.CONTROLLER,
  GrailsArtifact.SERVICE
)

private fun shouldBeDecorated(project: Project, file: VirtualFile): Boolean {
  val fileType = file.fileType
  if (fileType === GroovyFileType.GROOVY_FILE_TYPE) {
    if (file.extension == GsonConstants.EXTENSION || GrailsUtils.isInGrailsTests(file, project)) {
      return true
    }
    else {
      val classDefinition = GroovyUtils.getPublicClass(project, file)
      val artifact = GrailsArtifact.getType(classDefinition)
      if (DECORATED_ARTEFACT_TYPES.contains(artifact)) {
        return true
      }
    }
  }
  else if (fileType === GspFileType.GSP_FILE_TYPE || "jsp" == file.extension) {
    val controllerName = GrailsUtils.getControllerNameByGsp(file)
    if (controllerName != null && controllerName != "layouts" && StringUtil.isJavaIdentifier(controllerName)) {
      return true
    }
  }
  return false
}
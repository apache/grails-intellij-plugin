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

import com.intellij.openapi.util.NlsActions.ActionText
import com.intellij.openapi.util.NlsSafe
import com.intellij.psi.PsiClass
import org.jetbrains.plugins.grails.GrailsBundle
import org.jetbrains.plugins.grails.actions.ArtefactData
import org.jetbrains.plugins.grails.util.GrailsArtifact
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition
import javax.swing.Icon

abstract class GrailsGoToArtefactActionBase(private val artefactType: GrailsArtifact) : GrailsToolbarTargetAction<PsiClass>() {

  @NlsSafe override fun getTitle(artefactData: ArtefactData): String {
    return artefactData.artefactName.replaceFirstChar { it.uppercaseChar() } + artefactType.suffix
  }

  override fun getNavigateTargets(artefactData: ArtefactData): MutableCollection<GrClassDefinition> = artefactType.getInstances(
      artefactData.module, artefactData.packageName, artefactData.artefactName
  )


  @ActionText override fun getNavigateTitle(target: PsiClass): String =
    GrailsBundle.message("action.text.go.to.artefact", target.name)

  override fun getNavigateIcon(target: PsiClass): Icon? = artefactType.icon

  override fun navigate(artefactData: ArtefactData, target: PsiClass): Unit = target.navigate(true)
}

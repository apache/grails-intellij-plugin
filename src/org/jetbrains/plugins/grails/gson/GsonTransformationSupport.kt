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

import org.jetbrains.plugins.grails.GsonConstants
import org.jetbrains.plugins.grails.references.TraitInjectorService
import org.jetbrains.plugins.groovy.lang.psi.GroovyElementVisitor
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariableDeclaration
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrScriptField
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GroovyScriptClass
import org.jetbrains.plugins.groovy.transformations.AstTransformationSupport
import org.jetbrains.plugins.groovy.transformations.TransformationContext

class GsonTransformationSupport : AstTransformationSupport {

  private val VIEW_TYPE = "views"
  private val GSON_VIEW_TYPE = "view.gson"

  override fun applyTransformation(context: TransformationContext) {
    if (context.codeClass !is GroovyScriptClass) return
    val scriptClass = context.codeClass as GroovyScriptClass
    val file = scriptClass.containingFile
    if (!file.name.endsWith(GsonConstants.FILE_SUFFIX)) return

    findModelClosure(file)?.acceptChildren(object : GroovyElementVisitor() {
      override fun visitVariableDeclaration(variableDeclaration: GrVariableDeclaration) {
        for (variable in variableDeclaration.variables) {
          context.addField(GrScriptField(variable, scriptClass))
        }
      }
    })
    context.setSuperType("grails.plugin.json.view.JsonViewTemplate")
    val s = { fqn: String -> context.addInterface(fqn) }
    TraitInjectorService.getInjectedTraits(context.codeClass, VIEW_TYPE).forEach(s)
    TraitInjectorService.getInjectedTraits(context.codeClass, GSON_VIEW_TYPE).forEach(s)
  }
}

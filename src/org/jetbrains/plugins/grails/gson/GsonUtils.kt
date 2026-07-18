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

import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.plugins.grails.GsonConstants
import org.jetbrains.plugins.grails.util.GrailsUtils
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrVariable
import org.jetbrains.plugins.groovy.lang.psi.api.statements.blocks.GrClosableBlock
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.path.GrMethodCallExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMember
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrScriptField
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GroovyScriptClass

fun isGsonFile(file: PsiFile): Boolean = file.name.endsWith(GsonConstants.FILE_SUFFIX)

internal fun getModelFields(element: PsiElement?): List<GrScriptField> {
  return getModelFields(getScriptClass(element))
}

fun getScriptClass(element: PsiElement?): GroovyScriptClass? {
  val action = PsiTreeUtil.getParentOfType(element, GrField::class.java, GrMethod::class.java) as? GrMember ?: return null
  val gsonView = GrailsUtils.getViewPsiByAction(action).find { isGsonFile(it) }
  return (gsonView as? GroovyFile)?.scriptClass as? GroovyScriptClass
}

fun getModelFields(scriptClass: GroovyScriptClass?): List<GrScriptField> {
  if (scriptClass == null) return emptyList()
  return scriptClass.fields.filterIsInstance(GrScriptField::class.java)
}

fun isModelVariable(variable: PsiElement): Boolean {
  return variable is GrVariable && isModelVariable(variable)
}

fun isModelVariable(variable: GrVariable): Boolean {
  val closure = variable.parent?.parent as? GrClosableBlock ?: return false
  val file = variable.containingFile as? GroovyFile ?: return false
  return closure == findModelClosure(file)
}

fun findModelClosure(file: GroovyFile): GrClosableBlock? {
  for (statement in file.topStatements) {
    if (statement !is GrMethodCallExpression) continue
    if ("model" != (statement.invokedExpression as? GrReferenceExpression)?.referenceName) continue
    val arguments = statement.closureArguments
    return if (arguments.size == 1) arguments[0] else null
  }
  return null
}

fun isGsonTemplate(element: PsiElement?): Boolean = element is GroovyFile && element.isScript && element.virtualFile?.let {
  it.nameSequence.startsWith("_") && it.nameSequence.endsWith(GsonConstants.FILE_SUFFIX)
} ?: false

fun getGsonTemplateName(file: GroovyFile): String? = if (isGsonTemplate(file)) getGsonTemplateName(file.name) else null

fun getGsonTemplateName(fileName: String): String = fileName.removePrefix("_").removeSuffix(
  GsonConstants.FILE_SUFFIX)
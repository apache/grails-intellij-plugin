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

import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.psi.PsiType
import org.jetbrains.plugins.groovy.extensions.impl.NamedArgumentDescriptorBase
import org.jetbrains.plugins.groovy.lang.psi.GroovyPsiElement
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil

class GsonModelFieldArgumentDescriptor(val field: GrField) : NamedArgumentDescriptorBase() {

  override fun createReference(label: GrArgumentLabel): GsonControllerReference = GsonControllerReference(label)

  override fun checkType(type: PsiType, context: GroovyPsiElement): Boolean = TypesUtil.isAssignable(field.type, type, context)

  override fun customizeLookupElement(lookupElement: LookupElementBuilder): LookupElementBuilder =
      lookupElement.appendTailText(" Gson Model Field", true).withTypeText(field.type.presentableText)
}
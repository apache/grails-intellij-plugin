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

package org.jetbrains.plugins.grails.references.domain.detachedCriteria

import com.intellij.util.ProcessingContext
import groovy.lang.Closure
import org.jetbrains.plugins.groovy.lang.psi.api.GrFunctionalExpression
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression
import org.jetbrains.plugins.groovy.lang.psi.patterns.closureCallKey
import org.jetbrains.plugins.groovy.lang.psi.patterns.groovyClosure
import org.jetbrains.plugins.groovy.lang.psi.patterns.psiMethod
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.DelegatesToInfo
import org.jetbrains.plugins.groovy.lang.resolve.delegatesTo.GrDelegatesToProvider

class DetachedCriteriaDelegatesToProvider : GrDelegatesToProvider {

  private companion object {
    val closurePattern = groovyClosure().inMethod(
        psiMethod(
            "grails.gorm.DetachedCriteria",
            "and", "or", "not", // DetachedCriteria#handleJunction()
            "get", "list", "count", "exists", "find", // DetachedCriteria#withPopulatedQuery()
            "eqAll", "gtAll", "ltAll", "geAll", "leAll", // DetachedCriteria#buildQueryableCriteria()
            "build", "where" // DetachedCriteria#build()
        )
    )
  }

  override fun getDelegatesToInfo(expression: GrFunctionalExpression): DelegatesToInfo? {
    val context = ProcessingContext()
    if (!closurePattern.accepts(expression, context)) return null
    val call = context.get(closureCallKey) as? GrMethodCall
    val referenceExpression = call?.invokedExpression as? GrReferenceExpression
    return referenceExpression?.qualifierExpression?.type?.let {
      DelegatesToInfo(it, Closure.DELEGATE_FIRST)
    }
  }
}
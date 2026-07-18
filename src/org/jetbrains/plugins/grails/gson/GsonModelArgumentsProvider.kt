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

import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap
import org.jetbrains.plugins.groovy.lang.psi.controlFlow.ControlFlowBuilderUtil

class GsonModelArgumentsProvider : GroovyNamedArgumentProvider() {

  override fun getNamedArguments(literal: GrListOrMap): Map<String, NamedArgumentDescriptor> {
    if (!ControlFlowBuilderUtil.isCertainlyReturnStatement(literal)) return emptyMap()

    val modelFields = getModelFields(literal)
    if (modelFields.isEmpty()) return emptyMap()
    val result = mutableMapOf<String, NamedArgumentDescriptor>()

    for (field in modelFields) {
      result[field.name] = GsonModelFieldArgumentDescriptor(field)
    }
    return result
  }
}
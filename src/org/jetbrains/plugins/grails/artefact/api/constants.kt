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

package org.jetbrains.plugins.grails.artefact.api

import com.intellij.openapi.components.Service
import com.intellij.openapi.extensions.ExtensionPointName
import org.jetbrains.plugins.grails.artefact.impl.ControllerArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.DomainArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.FilterArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.InterceptorArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.ServiceArtefactHandler
import org.jetbrains.plugins.grails.artefact.impl.TaglibArtefactHandler

private val EP_NAME = ExtensionPointName<GrailsArtefactHandler>("org.intellij.grails.artefactHandler")

private val namedHandlers = sequenceOf<GrailsArtefactHandler>(
  DomainArtefactHandler,
  ControllerArtefactHandler,
  ServiceArtefactHandler,
  TaglibArtefactHandler,
  InterceptorArtefactHandler,
  FilterArtefactHandler
)

@Service(Service.Level.APP)
internal class HandlerCache {
  val idToHandler by lazy(LazyThreadSafetyMode.NONE) {
    allHandlers.associateBy { it.artefactHandlerID }
  }

  val annotationToHandler: Map<String, GrailsArtefactHandler> by lazy(LazyThreadSafetyMode.NONE) {
    allHandlers.associateByM { it.annotationFqns }
  }
}

internal val allHandlers: Sequence<GrailsArtefactHandler>
  get() {
    return namedHandlers.plus(EP_NAME.extensionList)
  }

internal val displayableArtefactHandlers: Sequence<GrailsDisplayableArtefactHandler>
  get() {
    return allHandlers.filterIsInstance(GrailsDisplayableArtefactHandler::class.java)
  }

private inline fun <T, K> Sequence<T>.associateByM(keySelector: (T) -> Iterable<K>): Map<K, T> {
  val result = mutableMapOf<K, T>()
  for (handler in this) {
    for (fqn in keySelector(handler)) {
      result[fqn] = handler
    }
  }
  return result
}
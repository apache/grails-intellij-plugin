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

package org.jetbrains.plugins.grails.artefact.api;

import com.intellij.openapi.components.Service;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.LinkedHashMap;
import java.util.Map;

@ApiStatus.Internal
@Service(Service.Level.APP)
public final class HandlerCache {

  // Computed on first use rather than in the constructor, because handlers contributed
  // through the extension point are not necessarily registered when the service is created.
  // The computations are idempotent, so a race just recomputes the same map; volatile is
  // only here to publish the result safely.
  private volatile Map<String, GrailsArtefactHandler> idToHandler;
  private volatile Map<String, GrailsArtefactHandler> annotationToHandler;

  public @NotNull Map<String, GrailsArtefactHandler> getIdToHandler() {
    Map<String, GrailsArtefactHandler> result = idToHandler;
    if (result == null) {
      result = new LinkedHashMap<>();
      for (GrailsArtefactHandler handler : ArtefactHandlers.allHandlers()) {
        result.put(handler.getArtefactHandlerID(), handler);
      }
      idToHandler = result;
    }
    return result;
  }

  /**
   * Maps every annotation FQN a handler claims to that handler. A handler may declare more
   * than one annotation, and a later handler claiming the same annotation wins.
   */
  public @NotNull Map<String, GrailsArtefactHandler> getAnnotationToHandler() {
    Map<String, GrailsArtefactHandler> result = annotationToHandler;
    if (result == null) {
      result = new LinkedHashMap<>();
      for (GrailsArtefactHandler handler : ArtefactHandlers.allHandlers()) {
        for (String fqn : handler.getAnnotationFqns()) {
          result.put(fqn, handler);
        }
      }
      annotationToHandler = result;
    }
    return result;
  }
}

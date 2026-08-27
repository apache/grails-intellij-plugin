/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.grails.intellij.plugin.artefact.api;

import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.artefact.impl.ControllerArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.impl.DomainArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.impl.FilterArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.impl.InterceptorArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.impl.ServiceArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.impl.TaglibArtefactHandler;

import java.util.ArrayList;
import java.util.List;

public final class ArtefactHandlers {

  private static final ExtensionPointName<GrailsArtefactHandler> EP_NAME =
    ExtensionPointName.create("org.intellij.grails.artefactHandler");

  private static final List<GrailsArtefactHandler> NAMED_HANDLERS = List.of(
    DomainArtefactHandler.INSTANCE,
    ControllerArtefactHandler.INSTANCE,
    ServiceArtefactHandler.INSTANCE,
    TaglibArtefactHandler.INSTANCE,
    InterceptorArtefactHandler.INSTANCE,
    FilterArtefactHandler.INSTANCE
  );

  private ArtefactHandlers() {
  }

  /**
   * The built-in handlers followed by any contributed through the extension point. The
   * extension list is read on every call, so handlers registered later are picked up.
   */
  public static @NotNull List<GrailsArtefactHandler> allHandlers() {
    List<GrailsArtefactHandler> result = new ArrayList<>(NAMED_HANDLERS);
    result.addAll(EP_NAME.getExtensionList());
    return result;
  }

  public static @NotNull List<GrailsDisplayableArtefactHandler> displayableArtefactHandlers() {
    List<GrailsDisplayableArtefactHandler> result = new ArrayList<>();
    for (GrailsArtefactHandler handler : allHandlers()) {
      if (handler instanceof GrailsDisplayableArtefactHandler displayable) {
        result.add(displayable);
      }
    }
    return result;
  }
}

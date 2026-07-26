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

package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler;
import org.jetbrains.plugins.grails.artefact.impl.ServiceArtefactHandler;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;

import java.util.List;

public class NewGrailsServiceAction extends NewGrailsXXXAction {

  public NewGrailsServiceAction() {
    super("action.Grails.NewService.text");
  }

  @Override
  protected @NotNull String getCommand(@NotNull GrailsApplication application) {
    return "create-service";
  }

  @Override
  protected @Nullable VirtualFile getTargetDirectory(@NotNull GrailsApplication application) {
    return GrailsArtifact.SERVICE.findDirectory(application);
  }

  @Override
  protected void fillGeneratedNamesList(@NotNull String name, @NotNull List<String> names) {
    names.add("grails-app/services/" + canonicalize(name) + "Service.groovy");
    names.add(GrailsUtils.GRAILS_INTEGRATION_TESTS + canonicalize(name) + "ServiceTests.groovy");
    names.add(GrailsUtils.GRAILS_UNIT_TESTS + canonicalize(name) + "ServiceSpec.groovy");
  }

  @Override
  protected @Nullable GrailsArtefactHandler getArtefactHandler() {
    return ServiceArtefactHandler.INSTANCE;
  }
}

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

package org.apache.grails.intellij.plugin.artefact.impl;

import com.intellij.icons.AllIcons;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.artefact.api.GrailsDisplayableArtefactHandler;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;

import javax.swing.Icon;

public final class ServiceArtefactHandler implements GrailsDisplayableArtefactHandler {

  public static final ServiceArtefactHandler INSTANCE = new ServiceArtefactHandler();

  private ServiceArtefactHandler() {
  }

  @Override
  public @NotNull String getArtefactHandlerID() {
    return "Service";
  }

  @Override
  public @Nullable VirtualFile getDirectory(@NotNull GrailsApplication application) {
    return application.getAppRoot().findChild("services");
  }

  @Override
  public @NotNull Icon getIcon() {
    return AllIcons.FileTypes.Config;
  }

  @Override
  public @NotNull String getTitle() {
    return "Services";
  }

  @Override
  public int getWeight() {
    return NodeWeights.SERVICES_FOLDER;
  }
}

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
package org.apache.grails.intellij.plugin.structure.impl;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.gradle.GrailsModuleData;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationBase;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.apache.grails.intellij.plugin.util.version.VersionImpl;

import java.util.Objects;

abstract class Grails3ApplicationBase extends GrailsApplicationBase implements Grails3Application {

  private final DataNode<ModuleData> myModuleDataNode;

  // Both derived from the external-system model, which does not change for a given node, so they
  // are computed once. Volatile publication only; the computation is idempotent.
  private volatile GrailsModuleData myGradleData;
  private volatile Version myGrailsVersion;

  protected Grails3ApplicationBase(@NotNull Project project,
                                   @NotNull VirtualFile root,
                                   @NotNull DataNode<ModuleData> moduleDataNode) {
    super(project, root);
    myModuleDataNode = moduleDataNode;
  }

  @Override
  public @NotNull String getName() {
    return myModuleDataNode.getData().getExternalName();
  }

  @Override
  public @Nullable String getAppVersion() {
    return myModuleDataNode.getData().getVersion();
  }

  @Override
  public @NotNull GrailsModuleData getGradleData() {
    GrailsModuleData result = myGradleData;
    if (result == null) {
      result = Objects.requireNonNull(ExternalSystemApiUtil.find(myModuleDataNode, GrailsModuleData.KEY)).getData();
      myGradleData = result;
    }
    return result;
  }

  @Override
  public @NotNull Version getGrailsVersion() {
    Version result = myGrailsVersion;
    if (result == null) {
      result = new VersionImpl(getGradleData().getGrailsVersion());
      myGrailsVersion = result;
    }
    return result;
  }

  @Override
  public boolean equals(Object other) {
    if (this == other) return true;
    if (other == null || other.getClass() != getClass()) return false;
    if (!super.equals(other)) return false;
    return myModuleDataNode.equals(((Grails3ApplicationBase)other).myModuleDataNode);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + myModuleDataNode.hashCode();
  }
}

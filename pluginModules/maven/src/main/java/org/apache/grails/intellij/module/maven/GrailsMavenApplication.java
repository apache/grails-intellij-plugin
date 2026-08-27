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

package org.apache.grails.intellij.module.maven;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenId;
import org.apache.grails.intellij.plugin.structure.impl.OldGrailsModuleBasedApplication;

final class GrailsMavenApplication extends OldGrailsModuleBasedApplication {

  private final @NotNull MavenId myMavenId;

  GrailsMavenApplication(@NotNull Module module, @NotNull VirtualFile root, @NotNull MavenId id) {
    super(module, root);
    myMavenId = id;
  }

  @Override
  public @NotNull String getName() {
    return StringUtil.notNullize(myMavenId.getArtifactId());
  }

  @Override
  public @Nullable String getAppVersion() {
    return myMavenId.getVersion();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    if (!super.equals(o)) return false;

    GrailsMavenApplication that = (GrailsMavenApplication)o;

    return myMavenId.equals(that.myMavenId);
  }

  @Override
  public int hashCode() {
    return 31 * super.hashCode() + myMavenId.hashCode();
  }
}

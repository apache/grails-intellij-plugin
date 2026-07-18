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

package com.intellij.groovy.grails.maven;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.idea.maven.model.MavenId;
import org.jetbrains.plugins.grails.structure.impl.OldGrailsModuleBasedApplication;

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

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

package org.apache.grails.intellij.plugin.sdk;

import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.version.Version;

public class GrailsSDK {

  private final @NotNull String myPath;
  private final @NotNull Version myVersion;

  public GrailsSDK(@NotNull String path, @NotNull Version version) {
    myPath = path;
    myVersion = version;
  }

  public @NotNull Version getVersion() {
    return myVersion;
  }

  public @NotNull String getPath() {
    return myPath;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    GrailsSDK sdk = (GrailsSDK)o;

    if (!myVersion.equals(sdk.myVersion)) return false;
    if (!myPath.equals(sdk.myPath)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    int result = myVersion.hashCode();
    result = 31 * result + (myPath.hashCode());
    return result;
  }

  @Override
  public String toString() {
    return String.format("GrailsSDK {myPath='%s', myVersion=%s}", myPath, myVersion);
  }
}

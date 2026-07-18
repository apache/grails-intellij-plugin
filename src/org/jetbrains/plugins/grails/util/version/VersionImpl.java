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

package org.jetbrains.plugins.grails.util.version;

import com.intellij.util.text.VersionComparatorUtil;
import org.jetbrains.annotations.NotNull;

public class VersionImpl implements Version {

  private final @NotNull String myVersionString;

  public VersionImpl(@NotNull String string) {
    myVersionString = string;
  }

  @Override
  public int compareTo(@NotNull Version o) {
    if (o instanceof VersionImpl) {
      return this.equals(o) ? 0 : VersionComparatorUtil.compare(myVersionString, ((VersionImpl)o).myVersionString);
    }
    else {
      return -o.compareTo(this);
    }
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    VersionImpl version = (VersionImpl)o;

    if (!myVersionString.equals(version.myVersionString)) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return myVersionString.hashCode();
  }

  @Override
  public String toString() {
    return myVersionString;
  }
}

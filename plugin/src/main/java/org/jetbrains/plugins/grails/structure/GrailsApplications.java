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
package org.jetbrains.plugins.grails.structure;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Comparator;

public final class GrailsApplications {

  /**
   * Orders applications by name, case-insensitively, with nulls first.
   *
   * <p>Was a top-level {@code @JvmField val} in structure/util.kt, so Java already referenced it
   * as {@code UtilKt.COMPARATOR}; it keeps the same shape as a static field here.
   */
  public static final Comparator<GrailsApplication> COMPARATOR = new Comparator<>() {
    @Override
    public int compare(@Nullable GrailsApplication app1, @Nullable GrailsApplication app2) {
      if (app1 == null && app2 == null) return 0;
      if (app1 == null) return -1;
      if (app2 == null) return 1;
      return app1.getName().compareToIgnoreCase(app2.getName());
    }
  };

  private GrailsApplications() {
  }

  static @NotNull Comparator<GrailsApplication> comparator() {
    return COMPARATOR;
  }
}

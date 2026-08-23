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

package org.apache.grails.intellij.plugin.lang.gsp;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.directive.GspDirective;

import java.util.HashMap;
import java.util.Map;

public enum GspDirectiveKind {
  PAGE("page", "tag"),
  INCLUDE("include"),
  TAGLIB("taglib"),
  ATTRIBUTE("attribute"),
  VARIABLE("variable");

  private final String[] tagNames;

  GspDirectiveKind(String ... tagNames) {
    this.tagNames = tagNames;
  }

  public String[] getTagNames() {
    return tagNames;
  }

  public boolean isInstance(@NotNull GspDirective directive) {
    return getKind(directive) == this;
  }

  public static @Nullable GspDirectiveKind getKind(@NotNull GspDirective directive) {
    return GspDirectiveKindStatic.KIND_MAP.get(directive.getName());
  }
}

final class GspDirectiveKindStatic {
  public static final Map<String, GspDirectiveKind> KIND_MAP = new HashMap<>();
  static {
    for (GspDirectiveKind kind : GspDirectiveKind.values()) {
      for (String tagName : kind.getTagNames()) {
        Object o = KIND_MAP.put(tagName, kind);
        assert o == null;
      }
    }
  }

  private GspDirectiveKindStatic() {
  }
}
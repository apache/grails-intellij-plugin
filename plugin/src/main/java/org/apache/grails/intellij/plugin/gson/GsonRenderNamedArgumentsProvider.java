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
package org.apache.grails.intellij.plugin.gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;

import java.util.Map;

public final class GsonRenderNamedArgumentsProvider extends GroovyNamedArgumentProvider {

  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    if (GsonPatterns.RENDER_METHOD_PATTERN_1.accepts(resolveResult.getElement())) {
      result.put("template", NamedArgumentDescriptor.TYPE_STRING);
      result.put("collection", NamedArgumentDescriptor.SIMPLE_ON_TOP);
      result.put("model", NamedArgumentDescriptor.TYPE_MAP);
      result.put("var", NamedArgumentDescriptor.TYPE_STRING);
      // result.put("bean", NamedArgumentDescriptor.SIMPLE_NORMAL);
    }
  }

  @Override
  public @NotNull Map<String, NamedArgumentDescriptor> getNamedArguments(@NotNull GrListOrMap literal) {
    if (GsonPatterns.RENDER_MAP_PARAMETER.accepts(literal)) {
      return Map.of("includes", NamedArgumentDescriptor.TYPE_LIST,
                    "excludes", NamedArgumentDescriptor.TYPE_LIST,
                    "deep", NamedArgumentDescriptor.TYPE_BOOL);
    }
    return Map.of();
  }
}

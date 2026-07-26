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
package org.jetbrains.plugins.grails.gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.controlFlow.ControlFlowBuilderUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrScriptField;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class GsonModelArgumentsProvider extends GroovyNamedArgumentProvider {

  @Override
  public @NotNull Map<String, NamedArgumentDescriptor> getNamedArguments(@NotNull GrListOrMap literal) {
    // Only the map a controller action returns describes the GSON model.
    if (!ControlFlowBuilderUtil.isCertainlyReturnStatement(literal)) return Map.of();

    List<GrScriptField> modelFields = GsonUtils.getModelFields(literal);
    if (modelFields.isEmpty()) return Map.of();

    Map<String, NamedArgumentDescriptor> result = new LinkedHashMap<>();
    for (GrScriptField field : modelFields) {
      result.put(field.getName(), new GsonModelFieldArgumentDescriptor(field));
    }
    return result;
  }
}

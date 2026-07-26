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

package org.jetbrains.plugins.grails.references.constraints;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;

import java.util.Map;

import static org.jetbrains.plugins.grails.references.constraints.GrailsConstraintNamedArgumentProvider.DESCRIPTORS;
import static org.jetbrains.plugins.grails.references.constraints.GrailsConstraintNamedArgumentProvider.Descriptor;
import static org.jetbrains.plugins.grails.references.constraints.GrailsConstraintNamedArgumentProvider.MinMaxArgumentDescriptor;
import static org.jetbrains.plugins.grails.references.constraints.GrailsConstraintNamedArgumentProvider.MyArgumentDescriptor;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.SIMPLE_ON_TOP;

public class GrailsConstraintGroupNamedArgumentProvider extends GroovyNamedArgumentProvider {

  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    PsiElement resolved = resolveResult.getElement();
    if (resolved == null) return;

    for (final Map.Entry<String, Descriptor> entry : DESCRIPTORS.entrySet()) {
      if (argumentName != null && !argumentName.equals(entry.getKey())) continue;

      String name = entry.getKey();

      if ("unique".equals(name)) continue;

      Object argumentDescriptorMarker = entry.getValue().marker();

      NamedArgumentDescriptor argumentDescriptor;

      if (argumentDescriptorMarker == MinMaxArgumentDescriptor.class) {
        argumentDescriptor = SIMPLE_ON_TOP;
      }
      else {
        argumentDescriptor = (NamedArgumentDescriptor)argumentDescriptorMarker;
      }

      result.put(name, new MyArgumentDescriptor(entry.getValue().constraintFn().apply(call), argumentDescriptor, resolved));
    }
  }
}

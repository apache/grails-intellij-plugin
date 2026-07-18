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

package org.jetbrains.plugins.grails.references.urlMappings;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.config.GrailsStructure;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.extensions.GroovyNamedArgumentProvider;
import org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor;
import org.jetbrains.plugins.groovy.lang.psi.api.GroovyResolveResult;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;

import java.util.Map;

import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.SIMPLE_ON_TOP;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.TYPE_BOOL;
import static org.jetbrains.plugins.groovy.extensions.NamedArgumentDescriptor.TYPE_CLASS;

public final class UrlMappingNamedArgumentProvider extends GroovyNamedArgumentProvider {

  // See DefaultUrlMappingEvaluator.UrlMappingBuilder#getURLMappingForNamedArgs()
  private static final Map<String, NamedArgumentDescriptor> map = GrailsUtils.createMap(
    "resource", SIMPLE_ON_TOP,
    "controller", SIMPLE_ON_TOP,
    "action", SIMPLE_ON_TOP,
    "view", SIMPLE_ON_TOP,
    "uri", SIMPLE_ON_TOP,
    "exception", TYPE_CLASS,
    "parseRequest", TYPE_BOOL
  );
  
  @Override
  public void getNamedArguments(@NotNull GrCall call,
                                @NotNull GroovyResolveResult resolveResult,
                                @Nullable String argumentName,
                                boolean forCompletion,
                                @NotNull Map<String, NamedArgumentDescriptor> result) {
    PsiElement resolve = resolveResult.getElement();
    if (resolve != null || !(call instanceof GrMethodCall)) return;

    if (!UrlMappingUtil.isMappingDefinition((GrMethodCall)call)) return;

    boolean isAtLeast230 = false;

    GrailsStructure structure = GrailsStructure.getInstance(call);
    if (structure != null && structure.isAtLeastGrails("2.3.0")) {
      isAtLeast230 = true;
    }

    if (argumentName == null) {
      result.putAll(map);

      if (isAtLeast230) {
        result.put("redirect", SIMPLE_ON_TOP);
      }
    }
    else {
      NamedArgumentDescriptor descriptor = map.get(argumentName);
      if (descriptor != null) {
        result.put(argumentName, descriptor);
      }
    }
  }
}

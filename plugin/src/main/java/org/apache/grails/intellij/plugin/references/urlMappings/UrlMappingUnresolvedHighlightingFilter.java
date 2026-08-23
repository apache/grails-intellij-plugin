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

package org.apache.grails.intellij.plugin.references.urlMappings;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.extensions.GroovyUnresolvedHighlightFilter;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrMethodCall;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrReferenceExpression;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrString;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.literals.GrStringInjection;

public final class UrlMappingUnresolvedHighlightingFilter extends GroovyUnresolvedHighlightFilter {
  @Override
  public boolean isReject(@NotNull GrReferenceExpression expression) {
    PsiElement parent = expression.getParent();

    if (parent instanceof GrStringInjection) {
      PsiElement gString = parent.getParent();
      if (!(gString instanceof GrString)) return false;

      PsiElement eMethodCall = gString.getParent();
      if (!(eMethodCall instanceof GrMethodCall methodCall)) return false;

      return UrlMappingUtil.isMappingDefinition(methodCall);
    }

    if (parent instanceof GrMethodCall) {
      return UrlMappingUtil.isMappingDefinition((GrMethodCall)parent);
    }

    return false;
  }
}

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

import com.intellij.patterns.PatternCondition;
import com.intellij.patterns.PsiMethodPattern;
import com.intellij.patterns.StandardPatterns;
import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.auxiliary.GrListOrMap;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.arguments.GrArgumentLabel;
import org.jetbrains.plugins.groovy.lang.psi.controlFlow.ControlFlowBuilderUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.literals.GrLiteralImpl;
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyElementPattern;
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyExpressionPattern;
import org.jetbrains.plugins.groovy.lang.psi.patterns.GroovyPatterns;
import org.jetbrains.plugins.groovy.lang.psi.util.GroovyCommonClassNames;

/**
 * PSI patterns shared by the GSON reference and named-argument providers. These were top-level
 * declarations spread across GsonControllerReferences.kt and GsonRenderReferences.kt.
 */
final class GsonPatterns {

  private GsonPatterns() {
  }

  private static final PatternCondition<PsiElement> RETURN_STATEMENT =
    new PatternCondition<>("is in return statement") {
      @Override
      public boolean accepts(@NotNull PsiElement element, @Nullable ProcessingContext context) {
        GrListOrMap statement = PsiTreeUtil.getParentOfType(element, GrListOrMap.class);
        return statement != null && ControlFlowBuilderUtil.isCertainlyReturnStatement(statement);
      }
    };

  /**
   * <pre>
   * def index() {
   *   return [&lt;place&gt;]
   * }
   *
   * def index() {
   *   [&lt;place&gt;]
   * }
   * </pre>
   */
  static final GroovyElementPattern.Capture<GrArgumentLabel> CONTROLLER_REFERENCE_PLACE =
    GroovyPatterns.namedArgumentLabel(null).with(RETURN_STATEMENT);

  private static final PsiMethodPattern RENDER_METHOD_BASE_PATTERN =
    new PsiMethodPattern().withName("render").definedInClass("grails.plugin.json.view.api.GrailsJsonViewHelper");

  /** GrailsJsonViewHelper#render(java.util.Map) */
  static final PsiMethodPattern RENDER_METHOD_PATTERN_1 =
    RENDER_METHOD_BASE_PATTERN.withParameters(CommonClassNames.JAVA_UTIL_MAP);

  /** GrailsJsonViewHelper#render(java.lang.Object, java.util.Map) */
  private static final PsiMethodPattern RENDER_METHOD_PATTERN_2 =
    RENDER_METHOD_BASE_PATTERN.withParameters(CommonClassNames.JAVA_LANG_OBJECT, CommonClassNames.JAVA_UTIL_MAP);

  /** GrailsJsonViewHelper#render(java.lang.Object, java.util.Map, groovy.lang.Closure) */
  private static final PsiMethodPattern RENDER_METHOD_PATTERN_3 =
    RENDER_METHOD_BASE_PATTERN.withParameters(CommonClassNames.JAVA_LANG_OBJECT,
                                              CommonClassNames.JAVA_UTIL_MAP,
                                              GroovyCommonClassNames.GROOVY_LANG_CLOSURE);

  /**
   * Second (java.util.Map) parameter of
   * GrailsJsonViewHelper#render(java.lang.Object, java.util.Map) and
   * GrailsJsonViewHelper#render(java.lang.Object, java.util.Map, groovy.lang.Closure).
   */
  static final GroovyExpressionPattern<GrListOrMap, ?> RENDER_MAP_PARAMETER =
    new GroovyExpressionPattern.Capture<>(GrListOrMap.class)
      .methodCallParameter(1, StandardPatterns.or(RENDER_METHOD_PATTERN_2, RENDER_METHOD_PATTERN_3));

  /** g.render(template: "&lt;place&gt;") */
  static final GroovyElementPattern.Capture<GrLiteralImpl> TEMPLATE_REFERENCE_PLACE =
    GroovyPatterns.stringLiteral().withParent(
      GroovyPatterns.namedArgument().withLabel("template").withParent(
        GroovyPatterns.groovyElement().withParent(
          GroovyPatterns.methodCall().withMethod(RENDER_METHOD_PATTERN_1))));
}

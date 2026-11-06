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

package org.apache.grails.intellij.plugin.gsp;

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.psi.util.PsiTypesUtil;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GrGspClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.expressions.GrExpression;

import java.util.Map;
import java.util.Set;

/**
 * Assertions on the implicit variables of a GSP ({@code request}, {@code params}, ...) and on the class a page
 * extends, shared by the tests for each Grails API.
 */
final class GspImplicitVariableAssertions {

  private GspImplicitVariableAssertions() {
  }

  /**
   * Asserts the type of each variable in a page that references them all, and that each type resolves to a class.
   *
   * @param expectedTypes   the fully qualified type of each variable
   * @param unresolvedTypes the variables whose type has no class on the test's classpath
   */
  static void assertVariableTypes(@NotNull GrailsTestCase test,
                                  @NotNull Map<String, String> expectedTypes,
                                  @NotNull Set<String> unresolvedTypes) {
    StringBuilder text = new StringBuilder();
    expectedTypes.keySet().forEach(variable -> text.append("${").append(variable).append("}\n"));
    PsiFile gsp = test.addView("ccc/index.gsp", text.toString());

    expectedTypes.forEach((variable, expectedType) -> {
      PsiType type = typeOf(gsp, variable);
      TestCase.assertEquals(variable, expectedType, type.getCanonicalText());
      if (!unresolvedTypes.contains(variable)) {
        TestCase.assertNotNull(variable + " should resolve to " + expectedType, PsiTypesUtil.getPsiClass(type));
      }
    });
  }

  /** Asserts that none of the variables resolves in a page that references them all. */
  static void assertNoVariables(@NotNull GrailsTestCase test, @NotNull String... variables) {
    StringBuilder text = new StringBuilder();
    for (String variable : variables) {
      text.append("${").append(variable).append("}\n");
    }
    PsiFile gsp = test.addView("ccc/absent.gsp", text.toString());

    for (String variable : variables) {
      int offset = gsp.getText().indexOf("${" + variable + "}") + 2;
      PsiReference reference = gsp.getViewProvider().findReferenceAt(offset, GroovyLanguage.INSTANCE);
      TestCase.assertNull(variable + " should not resolve", reference == null ? null : reference.resolve());
    }
  }

  /** The Groovy class a page compiles to. */
  static @NotNull GrGspClass pageClass(@NotNull PsiFile gsp) {
    GrGspClass page = PsiTreeUtil.findChildOfType(gsp.getViewProvider().getPsi(GroovyLanguage.INSTANCE), GrGspClass.class);
    TestCase.assertNotNull("no page class", page);
    return page;
  }

  /**
   * The type of the expression, which is what completion works from. A variable can resolve to a getter of the page's
   * superclass rather than to an implicit field, as {@code out} does.
   */
  private static @NotNull PsiType typeOf(@NotNull PsiFile gsp, @NotNull String variable) {
    int offset = gsp.getText().indexOf("${" + variable + "}") + 2;
    PsiReference reference = gsp.getViewProvider().findReferenceAt(offset, GroovyLanguage.INSTANCE);
    TestCase.assertNotNull("no reference to " + variable, reference);
    PsiType type = UsefulTestCase.assertInstanceOf(reference.getElement(), GrExpression.class).getType();
    TestCase.assertNotNull(variable + " does not resolve", type);
    return type;
  }
}

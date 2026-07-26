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

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.openapi.util.registry.Registry;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspCharsetReferenceTest extends LightJavaCodeInsightFixtureTestCase {
  public void testCharsetCompletion() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <%@ page contentType="text/html;charSet=<caret>" %>""");
    GrailsTestCase.checkCompletionStatic(myFixture, file, "UTF-8", "windows-1252");
  }

  public void testContentTypeCompletion() {
    Registry.get("ide.completion.variant.limit").setValue(10000, getTestRootDisposable());

    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <%@ page contentType=" <caret>" %>""");
    GrailsTestCase.checkCompletionStatic(myFixture, file, "text/html", "application/activemessage", "image/jpeg");
  }
}

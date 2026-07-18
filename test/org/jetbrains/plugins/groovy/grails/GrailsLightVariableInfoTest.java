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

package org.jetbrains.plugins.groovy.grails;

import com.intellij.codeInsight.navigation.CtrlMouseHandler;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Assert;

public class GrailsLightVariableInfoTest extends LightJavaCodeInsightFixtureTestCase {
  public void testInfo() {
    myFixture.configureByText("a.gsp", """      
      <g:each in="[1,2]" var="iii">
        ${i<caret>ii}
      </g:each>
      """);

    PsiReference ref = myFixture.getFile().findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());
    Assert.assertTrue(CtrlMouseHandler.getInfo(ref.resolve(), ref.getElement()).contains("Integer"));
  }
}

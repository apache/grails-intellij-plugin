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

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.util.ArrayList;
import java.util.Arrays;

public class GspControllerReferenceTest extends GrailsTestCase {
  public void testActionCompletion() {
    addController("class C1Controller { def actionC1={}}");
    addController("class C2Controller { def actionC2={}}");
    addController("class C3Controller { def actionC3={}}");

    configureByView("test/page.gsp", "<g:link controller='<caret>'/>");
    myFixture.complete(CompletionType.BASIC);
    TestCase.assertEquals(new ArrayList<>(Arrays.asList("c1", "c2", "c3")), myFixture.getLookupElementStrings());
  }

  public void testFieldActionRename() {
    addController("class C1Controller { def actionC1={}}");
    PsiFile controllerTest1 = myFixture.addFileToProject("test/unit/C1ControllerTest.groovy", "class C1ControllerTest { }");
    PsiFile controllerTest2 = myFixture.addFileToProject("test/unit/C1ControllerTests.groovy", "class C1ControllerTests { }");

    PsiFile file = configureByView("c1/page.gsp", "<g:link controller='c1<caret>'/>");

    myFixture.renameElementAtCaret("CccController");

    TestCase.assertEquals("<g:link controller='ccc'/>", file.getText());
    TestCase.assertEquals("ccc", file.getParent().getName());
    TestCase.assertEquals("CccControllerTest.groovy", controllerTest1.getName());
    TestCase.assertEquals("CccControllerTests.groovy", controllerTest2.getName());
  }

  public void testMakeNotController() {
    addController("class C1Controller { def actionC1={}}");
    configureByView("test/page.gsp", "<g:link controller='c1<caret>'");
    myFixture.renameElementAtCaret("Ccc");
    myFixture.checkResult("<g:link controller='c1'");
  }

  public void testDecapitalizeControllerName() {// All letters in controller name are upper case.
    addController("class CCCController { def actionC1={}}");
    configureByView("test/page.gsp", "<g:link controller='C<caret>'/>");
    myFixture.complete(CompletionType.BASIC);
    myFixture.checkResult("<g:link controller='CCC'/>");
    myFixture.renameElementAtCaret("RRRController");
    myFixture.checkResult("<g:link controller='RRR'/>");
  }
}

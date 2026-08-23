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

package org.apache.grails.intellij.plugin.tagSupport;

import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.util.List;

public class GspControllerAttributeTest extends GrailsTestCase {
  public void testActionCompletion() {
    addController("class C1Controller { def actionC1={}}");
    addController("class C2Controller { def actionC2={}}");
    addController("class C3Controller { def actionC3={}}");

    configureByView("test/page.gsp", "<g:link controller='<caret>'/>");

    myFixture.complete(CompletionType.BASIC);

    TestCase.assertEquals(List.of("c1", "c2", "c3"), myFixture.getLookupElementStrings());
  }

  public void testFieldActionRename() {
    addController("class C1Controller { def actionC1={}}");
    PsiFile controllerTest1 = myFixture.addFileToProject("test/unit/C1ControllerTest.groovy", "class C1ControllerTest { }");
    PsiFile controllerTest2 = myFixture.addFileToProject("test/unit/C1ControllerTests.groovy", "class C1ControllerTests { }");

    PsiFile file = myFixture.addFileToProject("grails-app/views/c1/page.gsp", "<g:link controller='c1<caret>'/>");

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.renameElementAtCaret("CccController");

    TestCase.assertEquals("<g:link controller='ccc'/>", file.getText());
    TestCase.assertEquals("ccc", file.getParent().getName());
    TestCase.assertEquals("CccControllerTest.groovy", controllerTest1.getName());
    TestCase.assertEquals("CccControllerTests.groovy", controllerTest2.getName());
  }

  public void testMakeNotController() {
    addController("class C1Controller { def actionC1={}}");

    PsiFile file = myFixture.addFileToProject("grails-app/views/test/page.gsp", "<g:link controller='c1<caret>'");

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.renameElementAtCaret("Ccc");

    TestCase.assertEquals("<g:link controller='c1'", file.getText());
  }

  public void testDecapitalizeControllerName() {// All letters in controller name are upper case.
    addController("class CCCController { def actionC1={}}");

    PsiFile file = myFixture.addFileToProject("grails-app/views/test/page.gsp", "<g:link controller='C<caret>'/>");

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    myFixture.complete(CompletionType.BASIC);

    TestCase.assertEquals("<g:link controller='CCC'/>", file.getText());

    myFixture.renameElementAtCaret("RRRController");

    TestCase.assertEquals("<g:link controller='RRR'/>", file.getText());
  }
}

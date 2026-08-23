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

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import static org.apache.grails.intellij.lib.testFramework.GrailsTestUtil.getTestRootPath;

public class GspActionAttributeTest extends GrailsTestCase {

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/gsp/");
  }

  public void testActionCompletion() {
    myFixture.copyFileToProject("TestController.groovy", "grails-app/controllers/TestController.groovy");

    addGroovyClass("grails-app/controllers", "package xxx; class Parent { def actionFromParent={}}");
    addController("package xxx; class TestController extends Parent { def namespace='NAMESPACE_MUST_BE_STATIC' def action10={}}");

    PsiFile file = addView("test/page.gsp", "<g:link action='<caret>'/>");

    checkCompletionVariants(file, "action10", "action4", "action5", "action6", "action7", "action8", "action9", "actionFromParent");
  }

  public void testFieldActionRename() {
    addController("class TestController { def action={}}");

    PsiFile file = myFixture.addFileToProject("grails-app/views/test/action.gsp",
                                              "<g:link action='action<caret>' /> <% link(action: 'action'); g.link(action: 'action') %>");
    PsiFile jspFile = myFixture.addFileToProject("grails-app/views/test/action.jsp", "");

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.renameElementAtCaret("newName");

    TestCase.assertEquals("<g:link action='newName' /> <% link(action: 'newName'); g.link(action: 'newName') %>", file.getText());
    TestCase.assertEquals("newName.gsp", file.getName());
    TestCase.assertEquals("action.jsp", jspFile.getName());
  }

  public void testActionWithoutControllerInController() {
    configureByController("""
                            class CccController {
                              def index = {
                                render link(action: '<caret>', "Link Text") as String
                              }
                            }
                            """);

    myFixture.completeBasic();
    myFixture.type("i\t");// Test for #IDEA-64293
    myFixture.checkResult("""
                            class CccController {
                              def index = {
                                render link(action: 'index', "Link Text") as String
                              }
                            }
                            """);
  }
}

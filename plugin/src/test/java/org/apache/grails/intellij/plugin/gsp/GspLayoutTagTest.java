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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GspLayoutTagTest extends GrailsTestCase {
  public void testRename() {
    PsiFile layout = myFixture.addFileToProject("grails-app/views/layouts/main.gsp", "");
    PsiFile page = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<meta name='layout' content='main'>");

    myFixture.renameElement(layout, "lll");
    TestCase.assertEquals("<meta name='layout' content='lll'>", page.getText());
  }

  public void testMove() {
    myFixture.addFileToProject("grails-app/views/layouts/ccc/main.gsp", "");
    PsiFile page = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<meta name='layout' content='ccc/main'>");

    myFixture.moveFile("grails-app/views/layouts/ccc/main.gsp", "grails-app/views/layouts");
    TestCase.assertEquals("<meta name='layout' content='main'>", page.getText());
  }

  public void testMakeNonLayout() {
    myFixture.addFileToProject("grails-app/views/layouts/ccc/main.gsp", "");
    PsiFile page = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<meta name='layout' content='ccc/main'>");

    myFixture.moveFile("grails-app/views/layouts/ccc/main.gsp", "grails-app/views");
    TestCase.assertEquals("<meta name='layout' content='ccc/main'>", page.getText());
  }

  public void testCompletion1() {
    myFixture.addFileToProject("grails-app/views/layouts/ccc/main.gsp", "");
    PsiFile page = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<meta name='layout' content='ccc/mai<caret>'>");

    myFixture.configureFromExistingVirtualFile(page.getVirtualFile());
    LookupElement[] res = myFixture.completeBasic();
    TestCase.assertNull(res);

    TestCase.assertEquals("<meta name='layout' content='ccc/main'>", page.getText());
  }

  public void testCompletion2() {
    myFixture.addFileToProject("grails-app/views/layouts/main.gsp", "");
    myFixture.addFileToProject("grails-app/views/layouts/lll.gsp", "");
    PsiFile file = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<meta name='layout' content='<caret>'>");
    checkCompletionVariants(file, "lll", "main");
  }

  public void testHighlighting() {
    myFixture.addFileToProject("grails-app/views/layouts/ccc/main.gsp", "");
    PsiFile file = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", """
      <meta name='layout' content='<warning descr="Cannot resolve file 'main'">main</warning>'>
      <meta name='layout' content='<warning descr="Cannot resolve file 'main.gsp'">main.gsp</warning>'>
      <meta name='layout' content='ccc/main'>
      <meta name='layout' content='ccc/main.gsp'>
      <meta name='layout' content='/ccc/main.gsp'>
      <meta name='layout' content='ccc/main.gsp/<warning descr="Cannot resolve file ''"></warning>'>
      <meta name='author' content='main'>
      <meta content='main'>
      """);

    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testApplyLayoutCompletion() {
    myFixture.addFileToProject("grails-app/views/layouts/main.gsp", "");
    myFixture.addFileToProject("grails-app/views/layouts/lll.gsp", "");
    PsiFile file = myFixture.addFileToProject("grails-app/views/ccc/page.gsp", "<g:applyLayout name=\"<caret>\"> </g:applyLayout>");
    checkCompletionVariants(file, "lll", "main");
  }
}

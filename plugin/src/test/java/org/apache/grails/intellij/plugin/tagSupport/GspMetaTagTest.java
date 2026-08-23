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

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.HddGrailsTestCase;

public class GspMetaTagTest extends HddGrailsTestCase {
  public void testRename() {
    VirtualFile propertiesFile = saveProperties("""                                
                                                  aaaa<caret>=1
                                                  """);
    PsiFile fileA = myFixture.addFileToProject("a.gsp", "<g:meta name='aaaa'/>");
    PsiFile fileB = myFixture.addFileToProject("b.gsp", "${g.meta(name: 'aaaa')}");
    PsiFile fileC = myFixture.addFileToProject("c.gsp", "<% out << meta(name: \"aaaa\") %>");
    //def fileD = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", "class CccController { def index = { out << meta(name: \"aaaa\") } }")
    PsiFile fileE = addController("class DddController { def index = { out << g.meta(name: 'aaaa') } }");

    myFixture.configureFromExistingVirtualFile(propertiesFile);

    myFixture.renameElementAtCaret("f");

    TestCase.assertEquals("<g:meta name='f'/>", fileA.getText());
    TestCase.assertEquals("${g.meta(name: 'f')}", fileB.getText());
    TestCase.assertEquals("<% out << meta(name: \"f\") %>", fileC.getText());
    //assertEquals "class CccController { def index = { out << meta(name: \"f\") } }", fileD.text
    TestCase.assertEquals("class DddController { def index = { out << g.meta(name: 'f') } }", fileE.getText());
  }

  public void testCompletion() {
    saveProperties("""
                     aaaa=1
                     bbbb=1
                     cccc=1
                     """);

    myFixture.addFileToProject("x.properties", """
      xxx=1
      yyy=1
      zzz=1
      """);

    myFixture.addFileToProject("a.gsp", "<g:meta name='<caret>'/>");
    myFixture.testCompletionVariants("a.gsp", "aaaa", "bbbb", "cccc");

    PsiFile controller = addController("class CccController { def index = { out << meta(name: \"<caret>\") } }");
    checkCompletionVariants(controller, "aaaa", "bbbb", "cccc");
  }
}

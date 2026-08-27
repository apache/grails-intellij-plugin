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

public class GspSelectTagTest extends GrailsTestCase {
  public void testCompletion() {
    myFixture.addFileToProject("Ggg.groovy", """
      class Ggg extends Parent {
        def property1 = "xxx"
        private zzz = "zzz"
        protected ppp = "ppp"
      
        public String getProperty2() {
          return ""
        }
      
        private String getNotAProperty() {
          return ""
        }
      }
      """);

    myFixture.addFileToProject("Parent.groovy", """
      class Parent {
        int parentProperty;
      
        public int getParentProperty2() {
          return 0;
        }
      }
      """);

    myFixture.addFileToProject("a.gsp", "<g:select from='${new Ggg()}' optionValue='<caret>' />");
    myFixture.testCompletionVariants("a.gsp", "metaClass", "parentProperty", "parentProperty2", "property1", "property2");

    myFixture.addFileToProject("b.gsp", "<% select(from: new Ggg(), optionValue: '<caret>')  %>");
    myFixture.testCompletionVariants("b.gsp", "metaClass", "parentProperty", "parentProperty2", "property1", "property2");
  }

  public void testRenameGroovyProperty() {
    myFixture.configureByText("Ggg.groovy", """
      
      class Ggg {
        def xxx<caret> = "sdsdsd"
      }
      """);
    PsiFile fileA = myFixture.addFileToProject("a.gsp", "<g:select from='${new Ggg()}' optionValue='xxx' />");
    PsiFile fileB = myFixture.addFileToProject("b.gsp", "<% select(from: new Ggg(), optionValue: 'xxx')  %>");
    PsiFile fileC = myFixture.addFileToProject("c.gsp", """
      <% select(from: new Ggg(), optionValue: "xxx")  %>""");

    myFixture.renameElementAtCaret("xxx777");

    TestCase.assertEquals("<g:select from='${new Ggg()}' optionValue='xxx777' />", fileA.getText());
    TestCase.assertEquals("<% select(from: new Ggg(), optionValue: 'xxx777')  %>", fileB.getText());
    TestCase.assertEquals("""
                            <% select(from: new Ggg(), optionValue: "xxx777")  %>""", fileC.getText());
  }

  public void testRenameGroovyGetter() {
    myFixture.configureByText("Ggg.groovy", """
      
      class Ggg {
        public String getXxx<caret>() {
          return "xxx"
        }
      }
      """);
    PsiFile fileA = myFixture.addFileToProject("a.gsp", "<g:select from='${new Ggg()}' optionValue='xxx' />");
    PsiFile fileB = myFixture.addFileToProject("b.gsp", "<% select(from: new Ggg(), optionValue: 'xxx')  %>");
    PsiFile fileC = myFixture.addFileToProject("c.gsp", """
      <% select(from: new Ggg(), optionValue: "xxx")  %>""");

    myFixture.renameElementAtCaret("getXxx777");

    TestCase.assertEquals("<g:select from='${new Ggg()}' optionValue='xxx777' />", fileA.getText());
    TestCase.assertEquals("<% select(from: new Ggg(), optionValue: 'xxx777')  %>", fileB.getText());
    TestCase.assertEquals("""
                            <% select(from: new Ggg(), optionValue: "xxx777")  %>""", fileC.getText());
  }

  public void testRenameJavaGetter() {
    myFixture.configureByText("Ggg.java", """
      public class Ggg {
        public String getXxx<caret>() {
          return "xxx"
        }
      }
      """);
    PsiFile fileA = myFixture.addFileToProject("a.gsp", "<g:select from='${new Ggg()}' optionValue='xxx' />");
    PsiFile fileB = myFixture.addFileToProject("b.gsp", "<% select(from: new Ggg(), optionValue: 'xxx')  %>");
    PsiFile fileC = myFixture.addFileToProject("c.gsp", """
      <% select(from: new Ggg(), optionValue: "xxx")  %>""");

    myFixture.renameElementAtCaret("getXxx777");

    TestCase.assertEquals("<g:select from='${new Ggg()}' optionValue='xxx777' />", fileA.getText());
    TestCase.assertEquals("<% select(from: new Ggg(), optionValue: 'xxx777')  %>", fileB.getText());
    TestCase.assertEquals("""
                            <% select(from: new Ggg(), optionValue: "xxx777")  %>""", fileC.getText());
  }

  public void testRenameJavaProperty() {
    myFixture.configureByText("Ggg.java", """
      public class Ggg {
      
        public String xxx<caret>;
      
      }
      """);
    PsiFile fileA = myFixture.addFileToProject("a.gsp", "<g:select from='${new Ggg()}' optionValue='xxx' />");
    PsiFile fileB = myFixture.addFileToProject("b.gsp", "<% select(from: new Ggg(), optionValue: 'xxx')  %>");
    PsiFile fileC = myFixture.addFileToProject("c.gsp", """
      <% select(from: new Ggg(), optionValue: "xxx")  %>""");

    myFixture.renameElementAtCaret("xxx777");

    TestCase.assertEquals("<g:select from='${new Ggg()}' optionValue='xxx777' />", fileA.getText());
    TestCase.assertEquals("<% select(from: new Ggg(), optionValue: 'xxx777')  %>", fileB.getText());
    TestCase.assertEquals("""
                            <% select(from: new Ggg(), optionValue: "xxx777")  %>""", fileC.getText());
  }

  public void testHighlighting() {
    configureByView("a.gsp", """
      <g:select name="grp" from="${[[key: 'Y', value: 'YES'], [key: 'N', value: 'NO']]}" optionKey="key" optionValue="value" />
      """);
    myFixture.checkHighlighting(true, false, true);
  }

  public void testMapKeysCompletion() {
    PsiFile file = addView("a.gsp", """
      <g:select name="grp" from="${[[key: 'Y', value: 'YES'], [key: 'N', value: 'NO']]}" optionKey="<caret>" optionValue="value" />
      """);
    checkCompletionVariants(file, "key", "value");
  }
}

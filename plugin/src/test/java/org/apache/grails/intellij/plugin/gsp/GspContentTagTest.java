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

package org.apache.grails.intellij.plugin.gsp;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInspection.htmlInspections.HtmlUnknownAttributeInspection;
import com.intellij.codeInspection.htmlInspections.HtmlUnknownTagInspection;
import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.junit.Assert;

public class GspContentTagTest extends GrailsTestCase {
  public void testHighlighting() {
    myFixture.enableInspections(HtmlUnknownTagInspection.class, HtmlUnknownAttributeInspection.class);

    PsiFile file = myFixture.addFileToProject("a.gsp", """
      
      <content tag="" <warning descr="Attribute fff is not allowed here">fff</warning>="">
        <div>
          Abc
        </div>
      </content>
      <<warning descr="Unknown html tag fgdfgdfgkdflgdf">fgdfgdfgkdflgdf</warning>>
        Abc
      </<warning descr="Unknown html tag fgdfgdfgkdflgdf">fgdfgdfgkdflgdf</warning>>
      """);
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testCompletionAttribute() {
    PsiFile file = myFixture.configureByText("a.gsp", """
      
      <content t<caret>
      </content>
      """);
    LookupElement[] res = myFixture.completeBasic();
    Assert.assertNull(res);

    Assert.assertEquals("""
                            
                            <content tag=""
                            </content>
                            """, file.getText());
  }

  public void testCompletionContent() {
    myFixture.configureFromExistingVirtualFile(myFixture.addFileToProject("a.gsp", """
      
      <content tag="">
        <inpu<caret>
      </content>
      """).getVirtualFile());
    myFixture.completeBasic();
    myFixture.assertPreferredCompletionItems(0, "input");
  }
}

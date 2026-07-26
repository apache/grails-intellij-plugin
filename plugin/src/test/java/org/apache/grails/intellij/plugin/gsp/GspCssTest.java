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

import com.intellij.psi.PsiFile;
import com.intellij.psi.css.inspections.invalid.CssInvalidPropertyValueInspection;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyProjectDescriptors;
import junit.framework.TestCase;
import org.apache.grails.intellij.plugin.fileType.GspFileType;

import java.util.List;

public class GspCssTest extends LightJavaCodeInsightFixtureTestCase {
  @Override
  protected @NotNull LightProjectDescriptor getProjectDescriptor() {
    return GroovyProjectDescriptors.MOCK_JDK_11;
  }

  public void testIDEA47622() {
    myFixture.configureByText("a.gsp", """
      <p style="margin-left:23423px;margin-right:45px">indent content</p>
      <p style="margin-left:<error descr="Mismatched property value (<anchor-size()>)"><error descr="Mismatched property value (<length-percentage> | auto)"><error descr="Mismatched property value (initial | inherit | unset | revert | revert-layer)">aas0px</error></error></error>;margin-right:4px">indent content</p>
      <p style="margin-left:0px;margin-right:<error descr="Mismatched property value (<anchor-size()>)"><error descr="Mismatched property value (<length-percentage> | auto)"><error descr="Mismatched property value (initial | inherit | unset | revert | revert-layer)">4</error></error></error>">indent content</p>
      """);
    myFixture.enableInspections(CssInvalidPropertyValueInspection.class);
    myFixture.checkHighlighting(true, false, true);
  }

  public void testAttributeClassReference() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <style type="text/css">
        .ccc<caret> { }
        .aaa { }
      </style>
      
      <g:link class="ccc" />
      <g:link class="aaa ccc" />
      <g:link class="aaa, ccc" />
      """);
    myFixture.renameElementAtCaret("zzzzz");

    TestCase.assertEquals("""
                            <style type="text/css">
                              .zzzzz { }
                              .aaa { }
                            </style>
                            
                            <g:link class="zzzzz" />
                            <g:link class="aaa zzzzz" />
                            <g:link class="aaa, zzzzz" />
                            """, file.getText());
  }

  public void testStyleAttribute() {
    myFixture.addFileToProject("a.gsp", "<g:link style='float: <caret>'/>");
    myFixture.testCompletionVariants("a.gsp", "inline-end", "inline-start", "left", "none", "revert-layer", "right", "inherit", "initial", "revert", "unset", "var()");
  }

  /**
   * Testing what IDEA should not inject CSS to 'style' attribute of 'g:formatDate' tag.
   * See #IDEA-65452 (Syntax error highlighted for style in g:formatDate (grails))
   */
  public void testFormatDateTag() {
    myFixture.addFileToProject("a.gsp", "<g:formatDate style='SHORT' type='date' date='${document?.lastUpdated}'/>");
    myFixture.testHighlighting(true, false, true, "a.gsp");
  }

  public void testHtmlTagCompletion() {
    myFixture.configureByText("a.gsp", "<a class='cssClass1, ${f<caret>}'>ref</a>");
    myFixture.completeBasic();
    myFixture.type("als\t");
    myFixture.checkResult("<a class='cssClass1, ${false<caret>}'>ref</a>");
  }

  public void testGspTagClassAttributeCompletion() {
    myFixture.configureByText("a.gsp", "<g:link class='cssClass1, ${f<caret>}'>ref</g:link>");
    myFixture.completeBasic();
    myFixture.type("als\t");
    myFixture.checkResult("<g:link class='cssClass1, ${false<caret>}'>ref</g:link>");
  }

  public void testGspTagIdAttributeCompletion() {
    myFixture.configureByText("a.gsp", "<g:link id='cssClass1, ${f<caret>}'>ref</g:link>");
    myFixture.completeBasic();
    myFixture.type("als\t");
    myFixture.checkResult("<g:link id='cssClass1, ${false<caret>}'>ref</g:link>");
  }

  public void testGroovyInjection() {
    myFixture.addFileToProject("a.gsp", "<div style='background-image: url(\"${<caret>}\")'></div>");

    final List<String> result = myFixture.getCompletionVariants("a.gsp");
    TestCase.assertNotNull(result);
    assert result.containsAll(List.of("List", "Character", "String"));
  }
}

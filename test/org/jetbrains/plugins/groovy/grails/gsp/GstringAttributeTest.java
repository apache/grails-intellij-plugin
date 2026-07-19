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

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.jetbrains.plugins.groovy.GroovyProjectDescriptors;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GstringAttributeTest extends LightJavaCodeInsightFixtureTestCase {
  @Override
  protected @NotNull LightProjectDescriptor getProjectDescriptor() {
    return GroovyProjectDescriptors.MOCK_JDK_11;
  }

  public void testHighlight() {
    myFixture.configureByText("a.gsp", """
      <g:link action="\\$" />
      <g:link action="$<error descr="Identifier expected">"</error> />
      <g:link action='\\$' />
      <g:link action='$<error descr="Identifier expected">'</error> />
      <g:link action='abc\\$1' />
      <g:link action='abc$<error descr="Identifier expected">1</error>' />
      <g:link action="abc$<error descr="Identifier expected">1</error>" />
      """);

    myFixture.testHighlighting("a.gsp");
  }

  public void testCompletion() {
    myFixture.addFileToProject("a.gsp", """
          <% def xxx1 = 1, xxx2 = 2 %>
          <g:each in="$xx<caret>" />
      """);
    myFixture.testCompletionVariants("a.gsp", "xxx1", "xxx2");

    myFixture.addFileToProject("b.gsp", """
          <% def xxx1 = 1, xxx2 = 2 %>
          <g:each in="abc$xx<caret>" />
      """);
    myFixture.testCompletionVariants("b.gsp", "xxx1", "xxx2");
  }

  public void testRename() {
    myFixture.configureByText("a.gsp", """      
          <% def xxx<caret> = 1 %>
          <g:each in="$xxx" />
          <g:link action="abc$xxx;dasdasdasdasd" />
          <g:link action="$xxx dasdasd" />
      """);

    myFixture.renameElementAtCaret("abc123");// new name is more then old name

    myFixture.checkResult("""
                                <% def abc123 = 1 %>
                                <g:each in="$abc123" />
                                <g:link action="abc$abc123;dasdasdasdasd" />
                                <g:link action="$abc123 dasdasd" />
                            """);

    myFixture.renameElementAtCaret("ttt");// new name is less then old name

    myFixture.checkResult("""
                                <% def ttt = 1 %>
                                <g:each in="$ttt" />
                                <g:link action="abc$ttt;dasdasdasdasd" />
                                <g:link action="$ttt dasdasd" />
                            """);
  }

  public void testEach() {
    PsiFile file = myFixture.addFileToProject("a.gsp", """
      <% def xxx = [1,2,3] %>
      <g:each in="$xxx">
        ${it.substring(1)}
        ${it.getInteger()}
      </g:each>
      """);

    GrailsTestCase.checkResolve(file.getViewProvider().getPsi(GroovyLanguage.INSTANCE), "getInteger");
  }
}

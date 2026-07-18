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

import com.intellij.codeInsight.documentation.DocumentationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GspTagDocumentationTest extends GrailsTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();

    addTaglib("""
                class MyTagLib {
                  /**
                   * @attr val The doc text.
                   */
                  def xxx = {
                
                  }
                }
                """);
  }

  public void testGspDoc() {
    PsiFile file = configureByView("a.gsp", "<g:xxx va<caret>l='1'/>");

    PsiElement originalElement = file.findElementAt(myFixture.getCaretOffset());
    assert "The doc text.".equals(getJavadoc(originalElement));
  }

  public void testGroovyDoc() {
    PsiFile file = configureByController("""
                                           class CccController {
                                             def foo = {
                                               xxx(val<caret>: 1)
                                             }
                                           }
                                           """);

    PsiElement originalElement = file.findElementAt(myFixture.getCaretOffset());
    assertEquals("The doc text.", getJavadoc(originalElement));
  }

  private String getJavadoc(@NotNull PsiElement originalElement) {
    PsiElement targetElement = DocumentationManager.getInstance(getProject())
      .findTargetElement(myFixture.getEditor(), myFixture.getFile(), originalElement);

    return DocumentationManager.getProviderFromElement(targetElement)
      .generateDoc(targetElement, originalElement);
  }
}

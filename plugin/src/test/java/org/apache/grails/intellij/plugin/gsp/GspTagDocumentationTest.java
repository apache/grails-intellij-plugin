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

import com.intellij.lang.documentation.ide.IdeDocumentationTargetProvider;
import com.intellij.platform.backend.documentation.DocumentationData;
import com.intellij.platform.backend.documentation.DocumentationTarget;
import com.intellij.platform.backend.documentation.impl.ImplKt;
import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.util.List;

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
    configureByView("a.gsp", "<g:xxx va<caret>l='1'/>");

    assertEquals("The doc text.", getJavadoc());
  }

  public void testGroovyDoc() {
    configureByController("""
                            class CccController {
                              def foo = {
                                xxx(val<caret>: 1)
                              }
                            }
                            """);

    assertEquals("The doc text.", getJavadoc());
  }

  private String getJavadoc() {
    PsiFile file = myFixture.getFile();
    List<? extends DocumentationTarget> targets = IdeDocumentationTargetProvider.getInstance(getProject())
      .documentationTargets(myFixture.getEditor(), file, myFixture.getCaretOffset());
    assertOneElement(targets);

    DocumentationData data = ImplKt.computeDocumentationBlocking(targets.get(0).createPointer());
    return data == null ? null : data.getHtml();
  }
}

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

import com.intellij.codeInsight.daemon.impl.analysis.HtmlUnknownTargetInspection;
import com.intellij.codeInsight.daemon.impl.analysis.XmlPathReferenceInspection;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GspCreateTemplateQuickFixTest extends GrailsTestCase {
  public void testCreateForControllerRender() {
    addController("""
                    class CccController {
                    }
                    """);

    PsiFile file = addView("ccc/a.gsp", "<g:render template='ttt<caret>'");
    runIntention(file, "Create template", true);
    TestCase.assertNotNull(file.getVirtualFile().getParent().findChild("_ttt.gsp"));
  }

  public void testCreateByAbsoluteUriRender() {
    addController("""
                    class CccController {
                    }
                    """);

    PsiFile file = addView("ccc/a.gsp", "<g:render template='/ttt<caret>'");
    runIntention(file, "Create template", true);
    TestCase.assertNotNull(file.getVirtualFile().getParent().getParent().findChild("_ttt.gsp"));
  }

  public void testCreateForControllerTmpl() {
    addController("""
                    class CccController {
                    }
                    """);

    PsiFile file = addView("ccc/a.gsp", "<tmpl:ttt<caret> />");
    runIntention(file, "Create template", true);
    TestCase.assertNotNull(file.getVirtualFile().getParent().findChild("_ttt.gsp"));
  }

  public void testCreateByAbsoluteUriTmpl() {
    addController("""
                    class CccController {
                    }
                    """);

    PsiFile file = addView("ccc/a.gsp", "<tmpl:/ttt<caret> />");
    runIntention(file, "Create template", true);
    TestCase.assertNotNull(file.getVirtualFile().getParent().getParent().findChild("_ttt.gsp"));
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.enableInspections(new XmlPathReferenceInspection(), new HtmlUnknownTargetInspection());
  }
}

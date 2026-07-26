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
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.lang.GroovyFormatterTestCase;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.apache.grails.intellij.lib.testFramework.GrailsTestUtil.getTestRootPath;

public class GspJsFormatterTest extends GroovyFormatterTestCase {

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/formatter/js/");
  }

  private void doTest() throws IOException {
    String jsCode = String.join("\n", Files.readAllLines(Paths.get(getTestDataPath(), getTestName(true) + ".test")));

    PsiFile a = myFixture.addFileToProject("a.gsp", "<g:javascript>\n" + jsCode + "\n</g:javascript>");
    PsiFile b = myFixture.addFileToProject("b.gsp", "<script>\n" + jsCode + "\n</script>");

    doFormat(a);
    doFormat(b);

    String textA = a.getText().replaceAll("</?g:javascript>", "");
    String textB = b.getText().replaceAll("</?script>", "");

    TestCase.assertEquals(textB, textA);
  }

  public void testT1() throws IOException { doTest(); }

  public void testT2() throws IOException { doTest(); }
}

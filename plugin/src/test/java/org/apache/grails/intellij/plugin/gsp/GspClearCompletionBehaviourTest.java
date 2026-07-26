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

import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GspClearCompletionBehaviourTest extends GrailsTestCase {
  public void testClearCompletionBehaviour() {
    PsiFile fileA = myFixture.configureByText("a.gsp", "<g<caret>");
    myFixture.completeBasic();// Completion variants from GspCompletionContributor
    myFixture.type("lin\n");

    PsiDocumentManager.getInstance(getProject()).commitAllDocuments();

    PsiFile fileB = myFixture.configureByText("b.gsp", "<g:lin<caret>");
    myFixture.completeBasic();// Completion variants from LegacyCompletionContributor
    myFixture.type("\n");

    TestCase.assertEquals(fileA.getText(), fileB.getText());
  }
}

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

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.TestCase;
import org.jetbrains.plugins.grails.fileType.GspFileType;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;

public class GspCreateVariableQuickFixTest extends LightJavaCodeInsightFixtureTestCase {
  public void testCreateVariableAndCodeBlock() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <div>
      \t${nonExistentVar<caret>}
      </div>
      """);
    myFixture.enableInspections(new GrUnresolvedAccessInspection());
    IntentionAction action = ContainerUtil.find(myFixture.getAvailableIntentions(),
                                                intention -> intention.getText().contains("Create variable"));
    TestCase.assertNotNull(action);
    myFixture.launchAction(action);

    TestCase.assertEquals("""
                            <%
                                def nonExistentVar
                            %>
                            <div>
                            \t${nonExistentVar}
                            </div>
                            """, file.getText());
  }

  public void testCreateVariable() {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <%
        def x = 12;
      %>
      <div>
      \t${nonExistentVar<caret>}
      </div>
      """);
    myFixture.enableInspections(new GrUnresolvedAccessInspection());
    IntentionAction action = ContainerUtil.find(myFixture.getAvailableIntentions(),
                                                intention -> intention.getText().contains("Create variable"));
    TestCase.assertNotNull(action);
    myFixture.launchAction(action);

    TestCase.assertEquals("""
                            <%
                              def x = 12;
                              def nonExistentVar
                            %>
                            <div>
                            \t${nonExistentVar}
                            </div>
                            """, file.getText());
  }
}

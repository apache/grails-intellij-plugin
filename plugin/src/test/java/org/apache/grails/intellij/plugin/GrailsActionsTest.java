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
package org.apache.grails.intellij.plugin;

import com.intellij.codeInsight.actions.MultiCaretCodeInsightAction;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.IdeActions;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

import static org.apache.grails.intellij.lib.testFramework.GrailsTestUtil.getTestRootPath;

public class GrailsActionsTest extends LightJavaCodeInsightFixtureTestCase {

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/actions/");
  }

  private void performMultiCaretCodeInsightAction(final String actionId) {
    MultiCaretCodeInsightAction action = (MultiCaretCodeInsightAction) ActionManager.getInstance().getAction(actionId);
    action.actionPerformedImpl(myFixture.getProject(), myFixture.getEditor());
  }

  public void testGspLineComment() { doTest(IdeActions.ACTION_COMMENT_LINE); }
  public void testGspLineUncomment() { doTest(IdeActions.ACTION_COMMENT_LINE); }
  public void testGspBlockComment() { doTest(IdeActions.ACTION_COMMENT_BLOCK); }

  private void doTest(String actionId) {
    myFixture.configureByFile(getTestName(false) + ".gsp");
    performMultiCaretCodeInsightAction(actionId);
    myFixture.checkResultByFile(getTestName(false) + "_after.gsp");
  }

}
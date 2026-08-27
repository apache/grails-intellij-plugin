/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.grails.intellij.plugin.i18n;

import com.intellij.codeInsight.intention.IntentionAction;
import org.apache.grails.intellij.module.i18n.GrailsI18nQuickFixHandler;
import com.intellij.openapi.util.Couple;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.util.IncorrectOperationException;
import org.apache.grails.intellij.plugin.fileType.GspFileType;

import java.util.List;

public class GspI18nIntentionTest extends LightJavaCodeInsightFixtureTestCase {

  private void doTest(String text) {
    doTest(text, null, null);
  }

  private void doTest(String text, String defaultPropertyValue, String args) {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, text);

    List<IntentionAction> intentions = myFixture.filterAvailableIntentions("Extract");
    if (intentions.isEmpty()) {
      assertNull(defaultPropertyValue);
      return;
    }

    try {
      GrailsI18nQuickFixHandler.INSTANCE.checkApplicability(file, myFixture.getEditor());
    }
    catch (IncorrectOperationException e) {
      assertNull(defaultPropertyValue);
      return;
    }

    Couple<String> pair = GrailsI18nQuickFixHandler.calculatePropertyValue(myFixture.getEditor(), file);
    assertNotNull(pair);

    assertEquals(defaultPropertyValue, pair.first);
    assertEquals(args, pair.second);
  }

  public void testIntention1() {
    doTest("\na<selection>aa\nsda</selection>\n", "aa\nsda", "");
  }

  public void testIntention2() {
    doTest("\na<selection>aa<% out << 1 %>sda</selection>\n");
  }

  public void testIntention21() {
    doTest("\na<selection>aasda${</selection>2}\n");
  }

  public void testIntention22() {
    doTest("\na${1<selection>}aasda</selection>\n");
  }

  public void testIntention3() {
    doTest("a<selection><div>aasda</div></selection>", "<div>aasda</div>", "");
  }

  public void testIntention4() {
    doTest("a<selection><g:link/></selection>");
  }

  public void testIntention5() {
    doTest("a<caret>aa");
  }

  public void testIntention6() {
    doTest("a<% 1 %><selection>${aaa}</selection><% out << \"aaa\" %>", "{0}", "aaa");
  }

  public void testIntention7() {
    doTest("a<selection>a ${aaa} s <%= 777 * 5   %> </selection> sdsa", "a {0} s {1} ", "aaa, 777 * 5");
  }

  public void testIntention8() {
    doTest("<selection>aaa${777}bbb${'!' + actionName + 'aaa'}</selection>", "aaa{0}bbb{1}",
           "777, '!' + actionName + 'aaa'");
  }

  public void testIntention9() {
    doTest("<selection>aaa${777}${777}</selection>", "aaa{0}{1}", "777, 777");
  }

  public void testIntention10() {
    doTest("<input class=\"button green medium\" type=\"button\" value=\"<selection>Subscribe</selection>\"/>",
           "Subscribe", "");
  }

  public void testIntention11() {
    doTest("<input class=\"button green medium\" type=\"button\" value=\"<selection>Subscribe ${aaa}</selection>\"/>",
           "Subscribe {0}", "aaa");
  }

  public void testIntention12() {
    doTest("<input class=\"button green medium\" type=\"button\" <selection>value=\"Subscribe</selection>\"/>");
  }
}

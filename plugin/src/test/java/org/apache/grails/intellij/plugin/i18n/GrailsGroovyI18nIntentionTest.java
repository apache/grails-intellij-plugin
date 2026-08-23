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
import org.apache.grails.intellij.module.i18n.GrailsI18nGroovyQuickFixHandler;
import com.intellij.openapi.util.Trinity;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.util.IncorrectOperationException;
import org.apache.grails.intellij.plugin.fileType.GspFileType;

import java.util.List;

public class GrailsGroovyI18nIntentionTest extends LightJavaCodeInsightFixtureTestCase {

  private void doTest(String text, String defaultPropertyValue) {
    doTest(text, defaultPropertyValue, "");
  }

  private void doTest(String text, String defaultPropertyValue, String args) {
    PsiFile file = myFixture.configureByText(GspFileType.GSP_FILE_TYPE, text);

    List<IntentionAction> intentions = myFixture.filterAvailableIntentions("Extract");
    if (intentions.isEmpty()) {
      assertNull(defaultPropertyValue);
      return;
    }

    try {
      GrailsI18nGroovyQuickFixHandler.INSTANCE.checkApplicability(file, myFixture.getEditor());
    }
    catch (IncorrectOperationException e) {
      assertNull(defaultPropertyValue);
      return;
    }

    Trinity<String, String, PsiElement> pair =
      GrailsI18nGroovyQuickFixHandler.calculatePropertyValue(myFixture.getEditor(), file);
    assertNotNull(pair);

    assertEquals(defaultPropertyValue, pair.first);
    assertEquals(args, pair.second);
  }

  public void testIntention1() {
    doTest("${\"<caret>aaa\"}", "aaa");
  }

  public void testIntention2() {
    doTest("${\"<selection>aaa 12</selection>\"}", "aaa 12");
  }

  public void testIntention3() {
    doTest("${<selection>\"aaa\"</selection>}", "aaa");
  }

  public void testIntention4() {
    doTest("${\"\"\"<selection>aaa</selection>\"\"\"}", "aaa");
  }

  public void testIntention5() {
    doTest("${<selection>\"\"\"aaa\"\"\"</selection>}", "aaa");
  }

  public void testIntention6() {
    doTest("${'<selection>aaa</selection>'}", "aaa");
  }

  public void testIntention7() {
    doTest("${<selection>'aaa'</selection>}", "aaa");
  }

  public void testIntentionFalse() {
    doTest("${'a<selection>a</selection>a'}", null);
  }

  public void testIntentionGString1() {
    doTest("${<selection>'a${777}aa'</selection>}", "a${777}aa");
  }

  public void testIntentionGString2() {
    doTest("${<selection>\"\"\"a${777}a${888}a\"\"\"</selection>}", "a{0}a{1}a", "777, 888");
  }

  public void testIntentionGString3() {
    doTest("${<selection>\"\"\"a${777}a${888}a\"\"\"</selection>}", "a{0}a{1}a", "777, 888");
  }

  public void testIntentionGString4() {
    doTest("${\"<caret>a${777}a${888}a\"}", "a{0}a{1}a", "777, 888");
  }

  public void testIntentionGStringExpression() {
    doTest("${\"<caret>a$a a\"}", "a{0} a", "a");
  }

  public void testIntentionGStringSum() {
    doTest("${\"<caret>a${a + 1} a\"}", "a{0} a", "a + 1");
  }
}

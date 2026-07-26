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

package org.apache.grails.intellij.plugin.parser;

import com.intellij.lang.html.HTMLLanguage;

import static org.apache.grails.intellij.lib.testFramework.GrailsTestUtil.getTestRootPath;

public class GspHtmlParserTest extends GspParsingTestCase {

  public void testAlone() { doTest(); }
  public void testCommon() { doTest(); }
  public void testHtml1() { doTest(); }
  public void testInject1() { doTest(); }
  public void testMmm2() { doTest(); }
  public void testMmm5() { doTest(); }
  public void testPeter1() { doTest(); }
  public void testPeter2() { doTest(); }
  public void testRange_parse() { doTest(); }
  public void testWerle() { doTest(); }
  public void testWerle28() { doTest(); }
  public void testWerle29() { doTest(); }
  public void testAfterWhitespace() { doTest(); }

  private void doTest() {
    doTest(HTMLLanguage.INSTANCE);
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/parser/gspHtml/");
  }

}
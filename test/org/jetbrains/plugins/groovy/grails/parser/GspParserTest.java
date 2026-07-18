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

package org.jetbrains.plugins.groovy.grails.parser;

import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GspParserTest extends GspParsingTestCase {
  public void testDir$dir1() { doTest(); }
  public void testInject$escaped1() { doTest(); }
  public void testInject$GRVY_943() { doTest(); }
  public void testSimple$bubug1() { doTest(); }
  public void testSimple$clos1() { doTest(); }
  public void testSimple$clos2() { doTest(); }
  public void testSimple$common() { doTest(); }
  public void testSimple$form1() { doTest(); }
  public void testSimple$megap1() { doTest(); }
  public void testSimple$mmm1() { doTest(); }
  public void testSimple$peter2() { doTest(); }
  public void testTags$act1() { doTest(); }
  public void testTags$act2() { doTest(); }
  public void testTags$gps9() { doTest(); }
  public void testTags$orph1() { doTest(); }
  public void testTags$tag3() { doTest(); }
  public void testTags$tagWithSlash() { doTest(); }
  public void testTags$uglyAttributeName() { doTest(); }
  public void testTags$unendedAttrList() { doTest(); }
  public void testHyphenInTagName() { doTest(); }

  private void doTest() {
    doTest(GspLanguage.INSTANCE);
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/parser/gsp/");
  }

}

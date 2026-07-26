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

import org.jetbrains.plugins.groovy.GroovyFileType;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GspGroovyParserTest extends GspParsingTestCase {

  public void testComments$com() { doTest(); }
  public void testComments$comm1() { doTest(); }
  public void testComments$commm2() { doTest(); }
  public void testControl$common() { doTest(); }
  public void testControl$for1() { doTest(); }
  public void testControl$for2() { doTest(); }
  public void testControl$if1() { doTest(); }
  public void testControl$if2() { doTest(); }
  public void testControl$swit1() { doTest(); }
  public void testControl$swit2() { doTest(); }
  public void testControl$foreach1() { doTest(); }
  public void testCustom$tag1() { doTest(); }
  public void testCustom$tag2() { doTest(); }
  public void testCustom$tag3() { doTest(); }
  public void testDeclarations$dec1() { doTest(); }
  public void testDeclarations$dec2() { doTest(); }
  public void testDeclarations$dec3() { doTest(); }
  public void testDeclarations$dec4() { doTest(); }
  public void testDirect$dir1() { doTest(); }
  public void testErrors$err1() { doTest(); }
  public void testErrors$err2() { doTest(); }
  public void testErrors$err3() { doTest(); }
  public void testErrors$err4() { doTest(); }
  public void testSimple$clos1() { doTest(); }
  public void testSimple$inj1() { doTest(); }
  public void testSimple$inj2() { doTest(); }
  public void testSimple$nl() { doTest(); }
  public void testSimple$stat1() { doTest(); }

  private void doTest() {
    doTest(GroovyFileType.GROOVY_FILE_TYPE.getLanguage());
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/parser/gspGroovy/");
  }

}

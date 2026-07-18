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
package org.jetbrains.plugins.groovy.grails.completion;

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.plugins.groovy.completion.CompletionTestBase;
import org.jetbrains.plugins.groovy.grails.GrailsTestUtil;
import org.jetbrains.plugins.groovy.util.TestUtils;

import static org.jetbrains.plugins.groovy.grails.GrailsTestUtil.getTestRootPath;

public class GspTagReferenceCompletionTest extends CompletionTestBase {

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/oldCompletion/gsp/");
  }

  @Override
  protected String getExtension() {
    return "gsp";
  }

  @Override
  protected void tuneFixture(JavaModuleFixtureBuilder fixtureBuilder) {
    fixtureBuilder.addLibraryJars("GRAILS", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-web-1.3.1.jar");
    fixtureBuilder.addLibrary("GROOVY", GrailsTestUtil.getMockGrailsLibraryHome() + '/' + TestUtils.GROOVY_JAR);
    fixtureBuilder.addLibraryJars("Grails", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-core-1.3.1.jar");

    String path = getTestRootPath("/testdata/mockTagLib");
    fixtureBuilder.addContentRoot(path).addSourceRoot("");
  }

  public void testAttr1() { doTest(); }
  public void testCustom1() { doTest(); }
  public void testCustomNamespacePrefix() { doTest(); }
  public void testDirname() { doTest(); }
  public void testG1() { doTest(); }
  public void testGet1() { doTest(); }
  public void testGroo1() { doTest(); }
  public void testGroo2() { doTest(); }
  public void testGroo3() { doTest(); }
  public void testGroo4() { doTest(); }
  public void testGroo5() { doTest(); }
  public void testHtml1() { doTest(); }
  public void testHtmlWithDoctype() { doTest(); }
  public void testLink() { doTest(); }
  public void testLink2() { doTest(); }
  public void testMy1() { doTest(); }
  public void testPackageTagLib1() { doTest(); }
  public void testPackageTagLib2() { doTest(); }
  public void testTail1() { doTest(); }
  public void testTail2() { doTest(); }
  public void testTail3() { doTest(); }
  public void testTail4() { doTest(); }
  public void testExcludeNotATagGroovy() { doTest(); }
  public void testExcludeNotATagHTML() { doTest(); }
  public void testEndTagTest1() { doTest(); }
  public void testEndTagTest2() { doTest(); }
  public void testEndTagTest3() { doTest(); }
  public void testEndTagTest4() { doTest(); }
  public void testEndTagTest5() { doTest(); }

  public void testNonStatic() { doTest(); }
}

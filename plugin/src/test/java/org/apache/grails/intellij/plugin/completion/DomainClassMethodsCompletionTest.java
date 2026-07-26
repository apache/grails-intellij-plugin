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

package org.apache.grails.intellij.plugin.completion;

import com.intellij.openapi.util.RecursionManager;
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.plugins.groovy.completion.CompletionTestBase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestUtil;
import org.jetbrains.plugins.groovy.util.TestUtils;

import static org.apache.grails.intellij.lib.testFramework.GrailsTestUtil.getTestRootPath;

public class DomainClassMethodsCompletionTest extends CompletionTestBase {

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    RecursionManager.assertOnRecursionPrevention(getTestRootDisposable());
  }

  public void testDyn1() { doTest(); }
  public void testFinder10() { doTest(); }
  public void testFinder11() { doTest(); }
  public void testFinder2() { doTest(); }
  public void testFinder3() { doTest(); }
  public void testFinder4() { doTest(); }
  public void testFinder5() { doTest(); }
  public void testFinder6() { doTest(); }
  public void testFinder7() { doTest(); }
  public void testFinder8() { doTest(); }
  public void testFinder_full() { doTest(); }
  public void testList_order() { doTest(); }
  public void testNot_ref() { doTest(); }
  public void testQulaified() { doTest(); }
  public void testStatic() { doTest(); }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/oldCompletion/domain/");
  }

  @Override
  protected void tuneFixture(JavaModuleFixtureBuilder fixtureBuilder) {
    fixtureBuilder.addLibraryJars("GRAILS", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-web-1.3.1.jar", "/dist/grails-core-1.1.jar");
    fixtureBuilder.addLibraryJars("GROOVY", GrailsTestUtil.getMockGrailsLibraryHome(), TestUtils.GROOVY_JAR);
    String path = getTestRootPath("/testdata/mockDomainDir");
    fixtureBuilder.addContentRoot(path).addSourceRoot("grails-app/domain");
  }
}

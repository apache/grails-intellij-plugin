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

import com.intellij.testFramework.builders.JavaModuleFixtureBuilder;
import org.jetbrains.plugins.groovy.completion.CompletionTestBase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestUtil;

import static org.apache.grails.intellij.lib.testFramework.GrailsTestUtil.getTestRootPath;

public class ControllerReferenceCompletionTest extends CompletionTestBase {

  public void testContr1Controller() { doTest("grails-app/controllers"); }
  public void testContr2Controller() { doTest("grails-app/controllers"); }
  public void testContr3Controller() { doTest("grails-app/controllers"); }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/oldCompletion/controllers");
  }

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    GrailsTestUtil.createGrailsApplication(myFixture);
  }

  @Override
  protected void tuneFixture(JavaModuleFixtureBuilder moduleBuilder) {
    moduleBuilder.addLibraryJars("GRAILS", GrailsTestUtil.getMockGrails11LibraryHome(), "/dist/grails-web-1.3.1.jar");
  }
}

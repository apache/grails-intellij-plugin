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
package org.apache.grails.intellij.plugin.tests;

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.plugin.tests.GrailsTestUtils;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

/**
 * Resolving a spec back to the artefact it tests. This is what the editor toolbar needs: without it
 * {@code GrailsActionUtil.getArtefactData} returns null and every toolbar action is inert while a
 * spec is open, even though the same toolbar works from the artefact side.
 */
public class GrailsTestedClassTest extends GrailsTestCase {

  /** Stub of the Grails 3+ trait; the real one comes from grails-testing-support. */
  private void addUnitTestTrait() {
    myFixture.addFileToProject("src/groovy/grails/testing/web/controllers/ControllerUnitTest.groovy", """

package grails.testing.web.controllers

trait ControllerUnitTest<T> {
}
""");
  }

  private static PsiClass singleClass(PsiFile file) {
    return ((GroovyFile)file).getClasses()[0];
  }

  /**
   * The type argument of the unit-test trait is the authoritative link, and the only one that works
   * when the spec name does not follow the {@code <Artefact>Spec} convention.
   */
  public void testResolvesFromUnitTestTraitTypeArgument() {
    addUnitTestTrait();
    addController("""

class BookController {
  def index() {}
}
""");
    PsiFile spec = myFixture.addFileToProject("test/unit/BookCtrlSpec.groovy", """

class BookCtrlSpec implements grails.testing.web.controllers.ControllerUnitTest<BookController> {
}
""");

    PsiClass tested = GrailsTestUtils.getTestedClass(singleClass(spec));
    assertNotNull("spec must resolve to its artefact through the trait type argument", tested);
    assertEquals("BookController", tested.getName());
  }

  /** The name-convention fallback, for specs that declare no trait. */
  public void testResolvesFromNameConvention() {
    addController("""

class BookController {
  def index() {}
}
""");
    PsiFile spec = myFixture.addFileToProject("test/unit/BookControllerSpec.groovy", """

class BookControllerSpec {
}
""");

    PsiClass tested = GrailsTestUtils.getTestedClass(singleClass(spec));
    assertNotNull("spec must resolve to its artefact by name", tested);
    assertEquals("BookController", tested.getName());
  }
}

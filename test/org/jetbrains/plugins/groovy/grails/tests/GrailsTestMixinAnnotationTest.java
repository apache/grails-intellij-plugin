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

package org.jetbrains.plugins.groovy.grails.tests;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsTestMixinAnnotationTest extends Grails14TestCase {
  public void testArtifactFieldExists() {
    addController("""
                    class DddController {
                      def index() {}
                    }
                    """);

    addDomain("""
                class Ddd {
                    String name;
                }
                """);

    PsiFile testFile = myFixture.addFileToProject("test/unit/TttTest.groovy", """
      @grails.test.mixin.TestFor(DddController)
      @grails.test.mixin.TestMixin(grails.test.mixin.domain.DomainClassUnitTestMixin)
      class TttTest {
        private void xxx() {
          <caret>
        }
      }
      """);

    checkCompletion(testFile, "initializeDatastoreImplementation()", "mockDomain()", "mockFor()", "log", "assertEquals");
  }

  public void testMixinArray() {
    addController("""
                    class DddController {
                      def index() {}
                    }
                    """);

    addDomain("""
                class Ddd {
                    String name;
                }
                """);

    PsiFile testFile = myFixture.addFileToProject("test/unit/TttTest.groovy", """
      @grails.test.mixin.TestFor(DddController)
      @grails.test.mixin.TestMixin([grails.test.mixin.domain.DomainClassUnitTestMixin, grails.test.mixin.web.ControllerUnitTestMixin])
      class TttTest {
        private void xxx() {
          <caret>
        }
      }
      """);
    checkCompletion(testFile, "initializeDatastoreImplementation()", "mockDomain()", "mockFor()", "log", "assertEquals");
    assertEquals(1, myFixture.getLookupElementStrings().stream().filter(e -> "mockController".equals(e)).count());
  }

  @Override
  protected boolean needJUnit() {
    return true;
  }
}

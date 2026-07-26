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

package org.apache.grails.intellij.plugin.tests;

import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.light.LightElement;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.GrFieldImpl;

public class GrailsTestForAnnotationTest extends Grails14TestCase {
  public void testArtifactFieldExists() {
    addDomain("""
                class Ddd {
                    String name;
                }
                """);

    PsiFile testFile = myFixture.addFileToProject("test/unit/TttTest.groovy", """
      import grails.test.mixin.*
      @TestFor(Ddd)
      class TttTest {
        private Ddd domain;
        private void xxx() {
          domai<caret>
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(testFile.getVirtualFile());

    myFixture.completeBasic();
    myFixture.type("\n");
    assertTrue(myFixture.getElementAtCaret() instanceof GrFieldImpl);
  }

  public void testArtifactFieldNotExists() {
    addDomain("""
                class Ddd {
                    String name;
                }
                """);

    PsiFile testFile = myFixture.addFileToProject("test/unit/TttTest.groovy", """
      import grails.test.mixin.*
      @TestFor(Ddd)
      class TttTest {
        private void xxx() {
          domai<caret>
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(testFile.getVirtualFile());

    myFixture.completeBasic();
    myFixture.type('\n');
    assertTrue(myFixture.getElementAtCaret() instanceof LightElement);
  }

  public void testMethodFromMixinClasses() {
    addController("""
                    class CccController {
                    }
                    """);

    PsiFile testFile = myFixture.addFileToProject("test/unit/TttTest.groovy", """
      import grails.test.mixin.*
      @TestFor(CccController)
      class TttTest {
        private void xxx() {
          <caret>
        }
      }
      """);
    checkCompletion(testFile, "model", "view", "configureGrailsWeb", "shouldFail", "assertEquals");
  }

  @Override
  protected boolean needJUnit() {
    return true;
  }
}

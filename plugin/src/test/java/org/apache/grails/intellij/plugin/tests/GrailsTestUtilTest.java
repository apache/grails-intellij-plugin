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

import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.TestCase;
import org.apache.grails.intellij.plugin.tests.GrailsTestUtils;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

import java.util.Collection;

public class GrailsTestUtilTest extends Grails14TestCase {
  public void testTestUtil() {
    PsiFile domain = addDomain("""
                                 package eee;
                                 class Ddd {}
                                 """);
    myFixture.addFileToProject("test/unit/xxx/DddTest.groovy", "package xxx;\n class DddTest {}");
    myFixture.addFileToProject("test/integration/xxx/yyy/DddIntegrationTest.groovy", "package xxx.yyy;\n class DddIntegrationTest {}");
    myFixture.addFileToProject("test/integration/fff/Fff.groovy", """
      package fff;
      
      import grails.test.mixin.TestFor;
      
      @TestFor(eee.Ddd)
      class Fff {
      
      }
      """);

    PsiClass domainClass = ((PsiClassOwner)domain).getClasses()[0];

    Collection<PsiClass> tests = GrailsTestUtils.getTestsForArtifact(domainClass, true);
    UsefulTestCase.assertSize(3, tests);

    for (PsiClass t : tests) {
      TestCase.assertEquals(domainClass, GrailsTestUtils.getTestedClass(t));
    }
  }
}

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

package org.apache.grails.intellij.plugin.pluginSupport;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GrailsBuildTestDataTest extends GrailsTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.addFileToProject("src/java/grails/buildtestdata/mixin/Build.java", """
      package grails.buildtestdata.mixin;
      public @interface Build {
          Class<?>[] value();
      }
      """);
  }

  public void testResolveOneDomain() {
    myFixture.enableInspections(GrUnresolvedAccessInspection.class);

    addDomain("""
                class Ddd { String name }
                """);
    addDomain("""
                class Ggg { String name }
                """);

    PsiFile file = myFixture.addFileToProject("test/unit/SomeTest.groovy", """
      @grails.buildtestdata.mixin.Build(Ddd)
      class SomeTest {
        def test() {
          Ggg.<warning>build</warning>()
          Ddd.<warning>unresolvedMethod</warning>()
          def ddd = Ddd.build()
          def x = ddd.name
          def y = ddd.<warning>unresolvedProperty</warning>
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }

  public void testResolveDomainList() {
    myFixture.enableInspections(GrUnresolvedAccessInspection.class);
    addDomain("""
                class Ddd { String name }
                """);
    addDomain("""
                class Ggg { String name }
                """);

    PsiFile file = myFixture.addFileToProject("test/unit/SomeTest.groovy", """
      @grails.buildtestdata.mixin.Build([Ddd, Ggg])
      class SomeTest {
        def test() {
          Ggg.build()
          Ddd.build()
          Ddd.<warning>foo</warning>()
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }

  public void testDomainPropertiesCompletion() {
    addDomain("""
                class Ddd {
                  String firstName
                  String secondName
                }
                """);

    PsiFile file = myFixture.addFileToProject("test/unit/SomeTest.groovy", """
      @grails.buildtestdata.mixin.Build(Ddd)
      class SomeTest {
        def test() {
          Ddd.build(<caret>)
        }
      }
      """);

    checkCompletion(file, "firstName", "secondName");
  }
}

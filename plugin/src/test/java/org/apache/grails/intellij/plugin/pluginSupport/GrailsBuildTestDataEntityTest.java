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
package org.apache.grails.intellij.plugin.pluginSupport;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;

/**
 * build-test-data 3.x+ puts build/findOrBuild on the entity itself, with no {@code @Build} mixin.
 * Signatures follow grails.buildtestdata.utils.MetaHelper#addBuildMetaMethods.
 */
public class GrailsBuildTestDataEntityTest extends GrailsTestCase {

  /** Stands in for the plugin being on the classpath. */
  private void addBuildTestDataToClasspath() {
    myFixture.addFileToProject("src/java/grails/buildtestdata/TestData.java", """
      package grails.buildtestdata;
      public class TestData {
      }
      """);
  }

  public void testBuildAndFindOrBuildResolveOnAnyEntity() {
    myFixture.enableInspections(GrUnresolvedAccessInspection.class);
    addBuildTestDataToClasspath();
    addDomain("""
                class Ddd { String name }
                """);

    PsiFile file = myFixture.addFileToProject("test/unit/SomeSpec.groovy", """
      class SomeSpec {
        def test() {
          def a = Ddd.build()
          def b = Ddd.build(name: 'x')
          def c = Ddd.findOrBuild()
          def d = Ddd.findOrBuild(name: 'x')
          def n = a.name
          Ddd.<warning>notARealMethod</warning>()
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }

  /**
   * The gate: build-test-data is a test dependency, so without it on the classpath the entity must
   * not gain the methods. This is what keeps them out of production code.
   */
  public void testNoMembersWhenPluginIsNotOnClasspath() {
    myFixture.enableInspections(GrUnresolvedAccessInspection.class);
    addDomain("""
                class Ddd { String name }
                """);

    PsiFile file = myFixture.addFileToProject("test/unit/SomeSpec.groovy", """
      class SomeSpec {
        def test() {
          Ddd.<warning>build</warning>()
          Ddd.<warning>findOrBuild</warning>()
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }

  public void testDomainPropertiesCompleteInsideBuild() {
    addBuildTestDataToClasspath();
    addDomain("""
                class Ddd { String name; Integer age }
                """);

    PsiFile file = myFixture.addFileToProject("test/unit/SomeSpec.groovy", """
      class SomeSpec {
        def test() {
          Ddd.build(<caret>)
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.completeBasic();
    assertContainsElements(myFixture.getLookupElementStrings(), "name", "age");
  }
}

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

package org.apache.grails.intellij.plugin.reference.spring;

import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GrailsSpringDSLTest extends Grails14TestCase {
  public void testResolveResourcesGroovy() {
    addSimpleGroovyFile("class Foo1 { String name; def xxx }");

    String textForResolve = """
        if (application.allArtefacts.length == 0) {
          bean1(Foo1) {
            name = ""
            xxx = ref("pluginManager")
            getParentCtx()
          }
        }
      
        ref("bean1")
        getParentCtx()
      
        unresolvedRef()
        unresolvedRef2(foo: '1')
      """;

    PsiFile resourcesFile = addResourcesGroovy("""
                                                 beans = {
                                                 """ + textForResolve + """
                                                 
                                                 }
                                                 """);

    GrailsTestCase.checkResolve(resourcesFile, "unresolvedRef", "unresolvedRef2");

    PsiFile pluginFile = addSimpleGroovyFile("""
                                               class MyGrailsPlugin {
                                                 def doWithSpring = {
                                               """ + textForResolve + """
                                               
                                                 }
                                               }
                                               """);

    GrailsTestCase.checkResolve(pluginFile, "unresolvedRef", "unresolvedRef2");
  }

  // todo failing, missing deps?!
  public void testHighlightUsages() {
    addSimpleGroovyFile("class Foo1 { String name }");

    addResourcesGroovy("""
                         beans = {
                           if (a == 1) {
                             bean1(Foo1)
                           }
                           else if (a == 2) {
                             bean1(Foo1)
                           }
                           else {
                             bean1<caret>(Foo1)
                           }
                         }
                         """);

    RangeHighlighter[] res = myFixture.testHighlightUsages("grails-app/conf/spring/resources.groovy");
    assertEquals(3, res.length);
  }

  public void testHighlighting() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    addSimpleGroovyFile("class Foo { String name }");
    addSimpleGroovyFile("""
                          class BeanClass {
                            String name
                            Foo foo
                            Foo foo2
                            Foo foo3
                          }
                          """);

    configureBySimpleGroovyFile("""
                                  class MyGrailsPlugin {
                                    def doWithSpring = {
                                      fooBean(Foo)
                                  
                                      bean(BeanClass) {
                                        name = ""
                                        foo = ref("fooBean")
                                        <warning>foo2</warning> = 2
                                        foo3 = new Foo()
                                      }
                                    }
                                  }
                                  """);

    myFixture.checkHighlighting(true, false, true);
  }

  private PsiFile addResourcesGroovy(String text) {
    return myFixture.addFileToProject("grails-app/conf/spring/resources.groovy", text);
  }
}

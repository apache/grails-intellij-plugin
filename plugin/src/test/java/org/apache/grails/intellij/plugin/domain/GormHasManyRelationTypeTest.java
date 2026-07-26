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

package org.apache.grails.intellij.plugin.domain;

import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GormHasManyRelationTypeTest extends GrailsTestCase {
  public void testRelationType() throws Exception {
    addDomain("""
                
                class A {
                    List<C> cs
                
                    static hasMany = [cs: C]
                }
                """);

    addDomain("class B extends A {}");
    addDomain("class C { }");

    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    configureBySimpleGroovyFile("""
                                  
                                  class Test {
                                  
                                    Test() {
                                      xxx(new B().cs)
                                      xxx(new A().cs)
                                  
                                      Set<C> s = aaa;
                                      xxx<warning>(s)</warning>
                                    }
                                  
                                    void xxx(List<C> param) {
                                  
                                    }
                                  }
                                  """);

    myFixture.checkHighlighting(true, false, true);
  }
}

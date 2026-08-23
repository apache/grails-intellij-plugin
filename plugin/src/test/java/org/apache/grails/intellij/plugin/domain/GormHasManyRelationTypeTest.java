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

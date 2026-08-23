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

public class GormMappingSortTest extends GrailsTestCase {
  @Override
  protected boolean needGormLibrary() {
    return true;
  }

  public void testCompletion() {
    configureByDomain("""
                        
                        class Ddd {
                          String firstName
                          String lastName
                        
                          static mapping = {
                            sort <caret>
                          }
                        }
                        """);

    checkCompletion("firstName:", "lastName:");
  }

  public void testHighlighting() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    configureByDomain("""
                        
                        class Ddd {
                          int firstName
                          int lastName
                        
                          static mapping = {
                            sort firstName: <warning>1</warning>, lastName: "zzz"
                          }
                        
                        }
                        """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testSortNamedArgumentCompletion() {
    configureByDomain("""
                        
                        class Ddd {
                          String name
                          static hasMany = [aaa: Ddd]
                        
                          static mapping = {
                            aaa(sort: '<caret>')
                          }
                        }
                        """);

    checkCompletion("name", "id");
  }
}

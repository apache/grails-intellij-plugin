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

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GrailsCriteriaBuilderToManyRelationTest extends GrailsTestCase {
  private void initDomains() {
    addDomain("""
                
                class Human {
                  String humanName;
                }
                """);

    addDomain("""
                
                class Street {
                  String streetName;
                  static hasMany = [humans: Human]
                }
                """);
    addDomain("""
                
                class City {
                  String cityName;
                  static hasMany = [streets: Street]
                }
                """);
  }

  @Override
  protected boolean needGormLibrary() {
    return true;
  }

  @Override
  protected boolean needHibernate() {
    return true;
  }

  public void testCompletion0() throws Exception {
    initDomains();

    configureBySimpleGroovyFile("""
                                  
                                  City.withCriteria {
                                    <caret>
                                  }
                                  """);

    checkCompletion("eq", "streets");
  }

  public void testCompletion1() throws Exception {
    initDomains();

    configureBySimpleGroovyFile("""
                                  
                                  City.withCriteria {
                                    streets {
                                      <caret>
                                    }
                                  }
                                  """);

    checkCompletion("eq", "humans");
  }

  public void testCompletion2() throws Exception {
    initDomains();

    configureBySimpleGroovyFile("""
                                  
                                  def c = City.createCriteria()
                                  c {
                                    streets {
                                      eq "<caret>"
                                    }
                                  }
                                  """);

    checkCompletion("streetName");
    checkNonExistingCompletionVariants("humanName", "cityName", "city");
  }

  public void testCompletion3() throws Exception {
    initDomains();

    configureBySimpleGroovyFile("""
                                  
                                  def c = City.createCriteria()
                                  c {
                                    streets {
                                      humans {
                                        eq "<caret>"
                                      }
                                    }
                                  }
                                  """);

    checkCompletion("humanName");
    checkNonExistingCompletionVariants("cityName", "streetName", "humans");
  }

  public void testRename1() throws Exception {
    initDomains();

    PsiFile file = addSimpleGroovyFile("""
                                         
                                         (City.createCriteria()) {
                                           streets {
                                             humans {
                                               eq "humanName", "Vasya"
                                             }
                                           }
                                         }
                                         """);

    configureBySimpleGroovyFile("new Human().humanName<caret>");
    myFixture.renameElementAtCaret("hhh");

    TestCase.assertEquals("""
                            
                            (City.createCriteria()) {
                              streets {
                                humans {
                                  eq "hhh", "Vasya"
                                }
                              }
                            }
                            """, file.getText());
  }

  public void testRename2() throws Exception {
    initDomains();

    PsiFile file = addSimpleGroovyFile("""
                                         
                                         (City.createCriteria()) {
                                           streets {
                                             humans {
                                               eq "humanName", "Vasya"
                                             }
                                           }
                                         }
                                         """);

    configureBySimpleGroovyFile("new Street().humans<caret>");
    myFixture.renameElementAtCaret("hhh");

    TestCase.assertEquals("""
                            
                            (City.createCriteria()) {
                              streets {
                                hhh {
                                  eq "humanName", "Vasya"
                                }
                              }
                            }
                            """, file.getText());
  }
}

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

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GormDetachedCriteriaTest extends GrailsTestCase {
  @Override
  protected boolean useGrails14() {
    return true;
  }

  public void testRename() throws Exception {
    addDomain("""
                
                class Ddd {
                  String name
                }
                """);

    configureBySimpleGroovyFile("""
                                  
                                  def x = new grails.gorm.DetachedCriteria<Ddd>(Ddd.class).build {
                                    eq "name", "Ivan"
                                    projections {
                                      if (true) {
                                        max("name")
                                      }
                                      eq "name", "1"
                                    }
                                  }
                                  
                                  x.and {
                                    eq "name", "Ivan"
                                  }
                                  
                                  x.eq("name", "Ivan")
                                  
                                  x = x.build({
                                    eq "name", "Ivan"
                                  })
                                  
                                  x.list(sort: 'name', {
                                      gt ""\"name""\", "a"
                                  })
                                  
                                  def z = Ddd.where {
                                      eq "name", "Ivan"
                                  }
                                  
                                  z.build {
                                    or {
                                      eq "name", "Vasya"
                                      or {
                                        eq "name", "Vasya"
                                      }
                                    }
                                  }
                                  
                                  Ddd.findAll [:], {
                                      eq "name", "Ivan"
                                      projections {
                                        if (true) {
                                          max("name")
                                        }
                                        eq "name", "1"
                                      }
                                  }
                                  
                                  Ddd.find {
                                      eq "name<caret>", "Ivan"
                                  }
                                  
                                  def g = new grails.gorm.DetachedCriteria<Ddd>(Ddd.class);
                                  g.updateAll(name: "Sergey")
                                  g.each { d ->
                                    println(d.name)
                                  }
                                  """);

    myFixture.renameElementAtCaret("firstName");

    myFixture.checkResult("""
                            
                            def x = new grails.gorm.DetachedCriteria<Ddd>(Ddd.class).build {
                              eq "firstName", "Ivan"
                              projections {
                                if (true) {
                                  max("firstName")
                                }
                                eq "firstName", "1"
                              }
                            }
                            
                            x.and {
                              eq "firstName", "Ivan"
                            }
                            
                            x.eq("firstName", "Ivan")
                            
                            x = x.build({
                              eq "firstName", "Ivan"
                            })
                            
                            x.list(sort: 'firstName', {
                                gt ""\"firstName""\", "a"
                            })
                            
                            def z = Ddd.where {
                                eq "firstName", "Ivan"
                            }
                            
                            z.build {
                              or {
                                eq "firstName", "Vasya"
                                or {
                                  eq "firstName", "Vasya"
                                }
                              }
                            }
                            
                            Ddd.findAll [:], {
                                eq "firstName", "Ivan"
                                projections {
                                  if (true) {
                                    max("firstName")
                                  }
                                  eq "firstName", "1"
                                }
                            }
                            
                            Ddd.find {
                                eq "firstName", "Ivan"
                            }
                            
                            def g = new grails.gorm.DetachedCriteria<Ddd>(Ddd.class);
                            g.updateAll(firstName: "Sergey")
                            g.each { d ->
                              println(d.firstName)
                            }
                            """);
  }

  public void testResolveDynamicFinderMethod() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String firstName;
                                 String lastName;
                               
                                 static {
                                   def criteria = where {
                                     isNotNull("firstName")
                                   }
                               
                                   criteria.findByLastNameAndVersionBetween("Ivanov", 1, 2)
                                 }
                               }
                               """);
    GrailsTestCase.checkResolve(file);
  }

  public void testCompletionDynamicFinders() {
    configureByDomain("""
                        
                        class Ddd {
                          String firstName;
                          String lastName;
                        
                          static {
                            def criteria = where {
                              isNotNull("firstName")
                            }
                        
                            criteria.findByLastNameAnd<caret>
                          }
                        }
                        """);
    checkCompletion("findByLastNameAndVersion", "findByLastNameAndId");
  }
}

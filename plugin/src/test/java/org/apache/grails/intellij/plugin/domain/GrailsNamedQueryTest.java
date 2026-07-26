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
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GrailsNamedQueryTest extends GrailsTestCase {
  public void testRenameNQWithoutParams() throws Exception {
    configureByDomain("""
                        
                        class Ddd {
                          String sss
                        
                          static namedQueries = {
                            myNamedQuery<caret> {
                              eq 'sss', "sss"
                            }
                          }
                        }
                        """);

    PsiFile testFile = addSimpleGroovyFile("class Test {{ Ddd.myNamedQuery.count() }}");

    myFixture.renameElementAtCaret("nq");

    myFixture.checkResult("""
                            
                            class Ddd {
                              String sss
                            
                              static namedQueries = {
                                nq {
                                  eq 'sss', "sss"
                                }
                              }
                            }
                            """);

    TestCase.assertEquals("class Test {{ Ddd.nq.count() }}", testFile.getText());
  }

  public void testRenameNQWithParams() throws Exception {
    configureByDomain("""
                        
                        class Ddd {
                          String sss
                        
                          static namedQueries = {
                            myNamedQuery<caret> { s ->
                              eq 'sss', s
                            }
                          }
                        }
                        """);

    PsiFile testFile = addSimpleGroovyFile("""
                                             
                                             Ddd.myNamedQuery.count();
                                             Ddd.myNamedQuery("nnn").count();
                                             """);

    myFixture.renameElementAtCaret("nq");

    myFixture.checkResult("""
                            
                            class Ddd {
                              String sss
                            
                              static namedQueries = {
                                nq { s ->
                                  eq 'sss', s
                                }
                              }
                            }
                            """);

    TestCase.assertEquals("""
                            
                            Ddd.nq.count();
                            Ddd.nq("nnn").count();
                            """, testFile.getText());
  }

  public void testRenameNQFromVariable() throws Exception {
    PsiFile domainFile = addDomain("""
                                     class Ddd {
                                       String sss
                                     
                                       static namedQueries = {
                                         myNamedQuery { s ->
                                           eq 'sss', s
                                         }
                                       }
                                     }
                                     """);

    configureBySimpleGroovyFile("""
                                  Ddd.myNamedQuery<caret>.count();
                                  Ddd.myNamedQuery("nnn").count();
                                  """);

    myFixture.renameElementAtCaret("nq");

    myFixture.checkResult("""
                            Ddd.nq.count();
                            Ddd.nq("nnn").count();
                            """);

    TestCase.assertEquals("""
                            class Ddd {
                              String sss
                            
                              static namedQueries = {
                                nq { s ->
                                  eq 'sss', s
                                }
                              }
                            }
                            """, domainFile.getText());
  }

  public void testHighlighting() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String aaa
                               
                                 static namedQueries = {
                                   qqq { s ->
                                     eq 'aaa', s
                                   }
                                 }
                               
                                 static {
                                   qqq.list("asda").add<warning descr="'add' in 'java.util.List<Ddd>' cannot be applied to '(java.lang.Integer)'">(1)</warning>
                                   qqq("sdas").listDistinct("asda").add<warning descr="'add' in 'java.util.List<Ddd>' cannot be applied to '(java.lang.Integer)'">(1)</warning>
                                   Ddd.qqq.findAllWhere([:]).add<warning descr="'add' in 'java.util.List<Ddd>' cannot be applied to '(java.lang.Integer)'">(1)</warning>
                                   Ddd.qqq.findAllWhere([:], false).add<warning descr="'add' in 'java.util.List<Ddd>' cannot be applied to '(java.lang.Integer)'">(1)</warning>
                                   Ddd.qqq.findAllWhere([:], true).aaa.substring<warning descr="'substring' in 'java.lang.String' cannot be applied to '(java.lang.String)'">("dd")</warning>
                                   Ddd.qqq.get(1).aaa.substring<warning descr="'substring' in 'java.lang.String' cannot be applied to '(java.lang.String)'">("dd")</warning>
                                   qqq("asdasd").count().byteValue<warning descr="'byteValue' in 'java.lang.Integer' cannot be applied to '(java.lang.Integer)'">(1212)</warning>
                                   qqq("aaa", {}).add<warning descr="'add' in 'java.util.List<Ddd>' cannot be applied to '(java.lang.String)'">("a")</warning>
                                   qqq("aaa", {
                                     projections {
                                       max("name")
                                     }
                                   }).add("a")
                                 }
                               }
                               """);
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testISSUE69231() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String aaa
                               
                                 static namedQueries = {
                                   withoutParamQuery {
                                     isNull("aaa")
                                   }
                                 }
                               }
                               """);
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testRenameChainedQueries() throws Exception {
    configureByDomain("""
                        
                        class Ddd {
                          String name;
                        
                          static namedQueries = {
                            xxx<caret> { p -> }
                            yyy {}
                          }
                        }
                        """);

    PsiFile file = addSimpleGroovyFile("Ddd.xxx.yyy.xxx('aaa').yyy.xxx.findAllWhere(name: 'ddd')");

    myFixture.renameElementAtCaret("z");

    TestCase.assertEquals("Ddd.z.yyy.z('aaa').yyy.z.findAllWhere(name: 'ddd')", file.getText());
  }

  public void testCompletionChainedQueries() throws Exception {
    addDomain("""
                
                class Ddd {
                  String name;
                
                  static namedQueries = {
                    xxx { p -> }
                    yyy {}
                  }
                }
                """);

    configureBySimpleGroovyFile("Ddd.xxx.yyy.xxx('aaa').<caret>");
    checkCompletion("findAllWhere", "@xxx", "xxx()", "@yyy", "yyy()");
  }

  @Override
  protected boolean needGormLibrary() {
    return true;
  }
}

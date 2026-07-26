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

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsDomainPropertiesTest extends GrailsTestCase {
  public void testRename() throws Exception {
    PsiFile fileSomeClass = addSimpleGroovyFile("""
                                                  
                                                  class SomeClass {
                                                   {
                                                    A a = new A()
                                                    a.manyProp = null
                                                   }
                                                  }
                                                  """);

    PsiFile fileA = addDomain("""
                                
                                class A {
                                  static hasMany = [manyProp<caret>: String]
                                }
                                """);

    myFixture.configureFromExistingVirtualFile(fileA.getVirtualFile());

    myFixture.renameElementAtCaret("ttt");

    TestCase.assertEquals("""
                            
                            class SomeClass {
                             {
                              A a = new A()
                              a.ttt = null
                             }
                            }
                            """, fileSomeClass.getText());

    TestCase.assertEquals("""
                            
                            class A {
                              static hasMany = [ttt: String]
                            }
                            """, fileA.getText());
  }

  public void testPropertyReferenceInListMethod() {
    configureByDomain("""
                        
                        class Ddd {
                          String firstName;
                          String lastName;
                        
                          static {
                            Ddd.list(sort:"<caret>")
                          }
                        }
                        """);

    checkCompletion("firstName", "lastName");
  }
}

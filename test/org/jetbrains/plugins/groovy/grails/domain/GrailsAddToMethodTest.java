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
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsAddToMethodTest extends GrailsTestCase {
  private void createTestClasses() {
    addDomain("""
                
                class City {
                
                  String name
                
                  static hasMany = [street: Street];
                }
                """);

    addDomain("""
                
                class Street {
                  String name
                
                  int index
                
                  static hasMany = [houseNumbers: Integer]
                }
                """);
  }

  public void testHighlighting() throws Exception {
    createTestClasses();
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);
    configureBySimpleGroovyFile("""
                                      def spb = new City(name: "Spb").save()
                                  
                                      spb.addToStreet(name: 'qqqRrr')
                                      spb.addToStreet(name: 12)
                                      spb.addToStreet(name: [1,2,3])
                                      spb.addToStreet(name: [:])
                                  
                                      spb.addToStreet(name: 'qqqRrr', index: <warning descr="Type of argument 'index' can not be 'String'">'Rrr'</warning>)
                                      spb.addToStreet(name: 'qqq12', index: 12)
                                      spb.addToStreet(name: 'qqq12', index: <warning descr="Type of argument 'index' can not be 'ArrayList<Integer>'">[1,2,3]</warning>)
                                  
                                      spb.addToStreet(name: 'qqq12', houseNumbers: [1,2,3,4,5])
                                      spb.addToStreet(name: 'qqq12', houseNumbers: <warning descr="Type of argument 'houseNumbers' can not be 'LinkedHashMap<String, Integer>'">['asda':232]</warning>)
                                      spb.addToStreet(name: 'qqq12', houseNumbers: <warning descr="Type of argument 'houseNumbers' can not be 'ArrayList<String>'">["1", '2']</warning>)
                                      spb.addToStreet(name: 'qqq12', houseNumbers: <warning descr="Type of argument 'houseNumbers' can not be 'String'">'asdasdasa'</warning>)
                                      spb.addToStreet(name: 'qqq12', houseNumbers: <warning descr="Type of argument 'houseNumbers' can not be 'Integer'">23</warning>)
                                  
                                      spb.addToStreet<warning>(1)</warning>
                                      spb.addToStreet(new Street())
                                      spb.removeFromStreet(new Street())
                                      spb.removeFromStreet<warning>("xxx")</warning>
                                  
                                      Street street = new Street()
                                      street.addToHouseNumbers(1)
                                      street.removeFromHouseNumbers(1)
                                      street.addToHouseNumbers<warning>([:])</warning>
                                  """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testCompletion() throws Exception {
    createTestClasses();

    configureBySimpleGroovyFile("""
                                    def spb = new City(name: "Spb").save()
                                    spb.addToStreet(<caret>: )
                                  """);

    checkCompletion("name", "index", "houseNumbers");
  }

  public void testRenameParameter() throws Exception {
    createTestClasses();

    PsiFile file = configureBySimpleGroovyFile("""
                                                 
                                                 def spb = new City(name: "Spb").save()
                                                 
                                                 spb.addToStreet(name<caret>: 'Nevskiy pr.')
                                                 spb.addToStreet(name: 'Lanskoe sh.')
                                                 
                                                 Street st = new Street()
                                                 println(st.name)
                                                 """);

    myFixture.renameElementAtCaret("zzz");

    TestCase.assertEquals("""
                            
                            def spb = new City(name: "Spb").save()
                            
                            spb.addToStreet(zzz: 'Nevskiy pr.')
                            spb.addToStreet(zzz: 'Lanskoe sh.')
                            
                            Street st = new Street()
                            println(st.zzz)
                            """, file.getText());
  }

  public void testRenameParameterCollection() throws Exception {
    createTestClasses();

    PsiFile file = configureBySimpleGroovyFile("""
                                                 
                                                 def spb = new City(name: "Spb").save()
                                                 
                                                 spb.addToStreet(name: 'Nevskiy pr.', houseNumbers<caret>: [1,2,3])
                                                 spb.addToStreet(name: 'Lanskoe sh.', houseNumbers: [1,2,3])
                                                 
                                                 Street st = new Street()
                                                 println(st.houseNumbers)
                                                 """);

    myFixture.renameElementAtCaret("zzz");

    TestCase.assertEquals("""
                            
                            def spb = new City(name: "Spb").save()
                            
                            spb.addToStreet(name: 'Nevskiy pr.', zzz: [1, 2, 3])
                            spb.addToStreet(name: 'Lanskoe sh.', zzz: [1, 2, 3])
                            
                            Street st = new Street()
                            println(st.zzz)
                            """, file.getText());

    TestCase.assertEquals("""
                            
                            class Street {
                              String name
                            
                              int index
                            
                              static hasMany = [zzz: Integer]
                            }
                            """, myFixture.findClass("Street").getContainingFile().getText());
  }

  public void testRenameField() {
    configureByDomain("""
                        
                        class City {
                        
                          String name
                        
                          static hasMany = [street<caret>: Street];
                        }
                        """);

    addDomain("""
                
                class Street {
                  String name
                
                  int index
                
                  static hasMany = [houseNumbers: Integer]
                }
                """);

    PsiFile file = addController("""
                                   
                                   class CccController {
                                     def index = {
                                       new City().addToStreet(new Street())
                                       new City().addToStreet([:])
                                       new City().removeFromStreet(null)
                                       new City().street
                                     }
                                   }
                                   """);

    myFixture.renameElementAtCaret("sss");

    TestCase.assertEquals("""
                            
                            class CccController {
                              def index = {
                                new City().addToSss(new Street())
                                new City().addToSss([:])
                                new City().removeFromSss(null)
                                new City().sss
                              }
                            }
                            """, file.getText());
  }
}

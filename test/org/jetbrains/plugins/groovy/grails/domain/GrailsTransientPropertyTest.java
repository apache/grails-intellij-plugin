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
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsTransientPropertyTest extends GrailsTestCase {
  public void _testCompletion() {
    myFixture.addFileToProject("src/java/aaa/Parent.java", """
      
      package aaa;
      
      public class Parent {
          public String getSss() {
              return "sss";
          }
      }
      """);

    PsiFile file = addDomain("""
                               
                               class City extends aaa.Parent {
                               
                                   String name
                                   int peopleCount
                                   Set<String> street;
                               
                                   static hasMany = [setOfString: String]
                               
                                   static transients = ["<caret>", 'street']
                               
                                   public String getSss2523() {
                               
                                   }
                               }
                               """);

    checkCompletionVariants(file, "name", "peopleCount", "sss2523", "sss");
  }

  public void testRenameMethod() {
    configureByDomain("""
                        
                        class City {
                        
                            String name
                            int peopleCount
                            Set<String> street;
                        
                            static transients = ["sss", 'street']
                        
                            public String getSss<caret>() {
                        
                            }
                        }
                        """);

    myFixture.renameElementAtCaret("getS");

    myFixture.checkResult("""
                            
                            class City {
                            
                                String name
                                int peopleCount
                                Set<String> street;
                            
                                static transients = ["s", 'street']
                            
                                public String getS() {
                            
                                }
                            }
                            """);
  }

  public void testRenameProperty() {
    configureByDomain("""
                        
                        class City {
                            String name
                            static transients = ["name<caret>"]
                        }
                        """);

    myFixture.renameElementAtCaret("cityName");

    myFixture.checkResult("""
                            
                            class City {
                                String cityName
                                static transients = ["cityName"]
                            }
                            """);
  }

  public void testCollectionPropertyWithoutHasManyIsTransient() {
    addDomain("""
                
                class City {
                    String name
                    Collection<String> street;
                }
                """);
    configureByController("""
                            
                            class CccController {
                              def index = {
                                City.withCriteria({
                                  eq "<caret>"
                                })
                              }
                            }
                            """);

    checkCompletion("id", "version", "name");
    checkNonExistingCompletionVariants("street");
  }

  @Override
  protected boolean needGormLibrary() {
    return true;
  }

  @Override
  protected boolean needHibernate() {
    return true;
  }
}

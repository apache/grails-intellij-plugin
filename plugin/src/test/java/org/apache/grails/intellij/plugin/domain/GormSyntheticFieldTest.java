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
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GormSyntheticFieldTest extends GrailsTestCase {
  public void testRenameSimpleProperty() {
    PsiFile d = addDomain("""
                            
                            class City {
                            
                              static hasMany = [streets<caret>: String]
                            
                              static constraints = {
                                streets(maxSize: 2)
                              }
                            }
                            """);
    PsiFile c = addController("""
                                
                                class CccController {
                                  def index = {
                                    def x = new City().streets
                                    def y = new City().getStreets()
                                    new City().setStreets([])
                                  }
                                }
                                """);
    PsiFile j = myFixture.addFileToProject("grails-app/controllers/Jjj.java", """
      
      public class Jjj {
        static {
          new City().getStreets()
          new City().setStreets(null)
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(d.getVirtualFile());

    myFixture.renameElementAtCaret("sss");

    TestCase.assertEquals("""
                            
                            class City {
                            
                              static hasMany = [sss: String]
                            
                              static constraints = {
                                sss(maxSize: 2)
                              }
                            }
                            """, d.getText());

    TestCase.assertEquals("""
                            
                            class CccController {
                              def index = {
                                def x = new City().sss
                                def y = new City().getSss()
                                new City().setSss([])
                              }
                            }
                            """, c.getText());

    TestCase.assertEquals("""
                            
                            public class Jjj {
                              static {
                                new City().getSss()
                                new City().setSss(null)
                              }
                            }
                            """, j.getText());
  }

  public void testRenamePropertyWithField() {
    PsiFile d = addDomain("""
                            
                            class City {
                              Set streets;
                            
                              static hasMany = [streets<caret>: String]
                            
                              static constraints = {
                                streets(maxSize: 2)
                              }
                            }
                            """);
    PsiFile c = addController("""
                                
                                class CccController {
                                  def index = {
                                    def x = new City().streets
                                    def y = new City().getStreets()
                                    new City().setStreets([])
                                  }
                                }
                                """);
    PsiFile j = myFixture.addFileToProject("src/java/Jjj.java", """
      
      public class Jjj {
        static {
          new City().getStreets()
          new City().setStreets(null)
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(d.getVirtualFile());

    myFixture.renameElementAtCaret("sss");

    TestCase.assertEquals("""
                            
                            class City {
                              Set sss;
                            
                              static hasMany = [sss: String]
                            
                              static constraints = {
                                sss(maxSize: 2)
                              }
                            }
                            """, d.getText());

    TestCase.assertEquals("""
                            
                            class CccController {
                              def index = {
                                def x = new City().sss
                                def y = new City().getSss()
                                new City().setSss([])
                              }
                            }
                            """, c.getText());

    TestCase.assertEquals("""
                            
                            public class Jjj {
                              static {
                                new City().getSss()
                                new City().setSss(null)
                              }
                            }
                            """, j.getText());
  }
}

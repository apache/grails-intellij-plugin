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

package org.apache.grails.intellij.plugin.reference.controller;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

public class GrailsActionWithExtensionTest extends Grails14TestCase {
  public void testRename() {
    PsiFile ddd = addController("""
                                  class DddController {
                                    def index() {
                                      link(controller: 'ccc', action: 'foo.xml')
                                    }
                                  }
                                  """);

    PsiFile view = addView("a.gsp", "<g:link controller='ccc' action='foo.xml'>");

    PsiFile ccc = configureByController("""
                                          class CccController {
                                              def foo<caret>() {
                                                withFormat {
                                                  html {}
                                                  xml {}
                                                }
                                              }
                                          
                                              def index = {
                                                redirect(action: 'foo.html')
                                              }
                                          }
                                          """);

    myFixture.renameElementAtCaret("z");

    assertEquals("""
                   class DddController {
                     def index() {
                       link(controller: 'ccc', action: 'z.xml')
                     }
                   }
                   """, ddd.getText());
    assertEquals("<g:link controller='ccc' action='z.xml'>", view.getText());

    assertEquals("""
                   class CccController {
                       def z() {
                         withFormat {
                           html {}
                           xml {}
                         }
                       }
                   
                       def index = {
                         redirect(action: 'z.html')
                       }
                   }
                   """, ccc.getText());
  }

  public void testCompletion() {
    configureByController("""
                            class CccController {
                                def foo() {
                                  withFormat {
                                    html {}
                                    xml {}
                                  }
                                }
                            
                                def index = {
                                  redirect(action: 'fo<caret>.html')
                                }
                            }
                            """);

    myFixture.completeBasic();

    myFixture.checkResult("""
                            class CccController {
                                def foo() {
                                  withFormat {
                                    html {}
                                    xml {}
                                  }
                                }
                            
                                def index = {
                                  redirect(action: 'foo.html')
                                }
                            }
                            """);
  }
}

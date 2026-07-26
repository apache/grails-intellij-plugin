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

package org.apache.grails.intellij.plugin.action;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

public class GrailsGenerateAction2Test extends Grails14TestCase {
  public void testGenerateFromGsp1() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<g:link action='xxx<caret>'");
    runIntention(gsp, "Create action", true);
    TestCase.assertEquals("""
                            class CccController {
                            
                                def xxx() {}
                            }
                            """, ccc.getText());
  }

  public void testGenerateFromGsp2() {
    PsiFile ccc = addController("""
                                  class CccController {
                                      def aaa() {
                                      }
                                  }
                                  """);
    PsiFile gsp = addView("ccc/a.gsp", "<g:link action='xxx<caret>'");
    runIntention(gsp, "Create action", true);
    TestCase.assertEquals("""
                            class CccController {
                                def aaa() {
                                }
                            
                                def xxx() {}
                            }
                            """, ccc.getText());
  }

  public void testGenerateFromGsp3() {
    PsiFile ccc = addController("""
                                  class CccController {
                                      def aaa() {
                                      }
                                      static def foo() {
                                      }
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<g:link action='xxx<caret>'");
    runIntention(gsp, "Create action", true);
    TestCase.assertEquals("""
                            class CccController {
                                def aaa() {
                                }
                            
                                def xxx() {}
                            
                                static def foo() {
                                }
                            }
                            """, ccc.getText());
  }

  public void testGenerateFromGsp4() {
    PsiFile ccc = addController("""
                                  class CccController {
                                      def aaa = {
                                  
                                      }
                                      static def foo() {
                                      }
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<g:link action='xxx<caret>'");
    runIntention(gsp, "Create action", true);
    TestCase.assertEquals("""
                            class CccController {
                                def aaa = {
                            
                                }
                                def xxx = {}
                            
                                static def foo() {
                                }
                            }
                            """, ccc.getText());
  }
}

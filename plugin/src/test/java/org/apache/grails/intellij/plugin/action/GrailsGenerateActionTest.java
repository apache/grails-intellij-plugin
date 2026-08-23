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

package org.apache.grails.intellij.plugin.action;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.junit.Assert;

public class GrailsGenerateActionTest extends GrailsTestCase {

  public void testGenerateFromGsp1() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<g:link action='xxx<caret>'");

    runIntention(gsp, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def xxx = {}
                          }
                          """, ccc.getText());
  }

  public void testGenerateFromGsp2() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                  }
                                  """);

    PsiFile gsp = addView("fff/a.gsp", "<g:link controller='ccc' action='xx<caret>x'");

    runIntention(gsp, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def xxx = {}
                          }
                          """, ccc.getText());
  }

  public void testGenerateFromGsp3() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<% link(action:'xxx<caret>') %>");

    runIntention(gsp, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def xxx = {}
                          }
                          """, ccc.getText());
  }

  public void testNotAnIdentifier() {
    addController("""
                    class CccController {
                    
                    }
                    """);

    PsiFile gsp = addView("fff/a.gsp", "<g:link controller='ccc' action='xx<caret>x asda 0'");

    runIntention(gsp, "Create Action", false);
  }

  public void testGenerateFromController() {
    PsiFile ccc = addController("""
                                  class CccController {
                                    def index = {
                                      redirect(action: 'xxx<caret>')
                                    }
                                  }
                                  """);

    runIntention(ccc, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                            def index = {
                              redirect(action: 'xxx')
                            }
                              def xxx = {}
                          }
                          """, ccc.getText());
  }

  public void testGenerateFromTaglib() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                  }
                                  """);

    PsiFile taglib = addTaglib("""
                                 class TttTagLib {
                                   def ttt = {
                                     link(action: "<caret>xxx", controller: 'ccc')
                                   }
                                 }
                                 """);

    runIntention(taglib, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def xxx = {}
                          }
                          """, ccc.getText());
  }

  public void testFormatterClosure1() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                      def action0 = {
                                      }
                                  
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<% link(action:'xxx<caret>') %>");

    runIntention(gsp, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def action0 = {
                              }
                              def xxx = {}
                          
                          }
                          """, ccc.getText());
  }

  public void testFormatterClosure2() {
    PsiFile ccc = addController("""
                                  class CccController {
                                  
                                      def action0 = {
                                      }
                                  
                                      private static def foo() {
                                      }
                                  }
                                  """);

    PsiFile gsp = addView("ccc/a.gsp", "<% link(action:'xxx<caret>') %>");

    runIntention(gsp, "Create action", true);

    Assert.assertEquals("""
                          class CccController {
                          
                              def action0 = {
                              }
                              def xxx = {}
                          
                              private static def foo() {
                              }
                          }
                          """, ccc.getText());
  }
}

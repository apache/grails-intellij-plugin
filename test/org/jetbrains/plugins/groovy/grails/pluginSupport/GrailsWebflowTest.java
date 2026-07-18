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

package org.jetbrains.plugins.groovy.grails.pluginSupport;

import com.intellij.codeInsight.daemon.GutterMark;
import com.intellij.openapi.editor.markup.RangeHighlighter;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class GrailsWebflowTest extends GrailsTestCase {
  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    try {
      VirtualFile applicationProperties = myFixture.getTempDirFixture().findOrCreateDir("application.properties");
      applicationProperties.setBinaryContent("plugins.webflow=0.5.5.1".getBytes(StandardCharsets.UTF_8));
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void testWebFlowActionName() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       redirect(action:'<caret>')
                                     }
                                   
                                     def shoppingCartFlow = {
                                       getBooks {}
                                       xxx {}
                                     }
                                   
                                     def zzz = {}
                                   }
                                   """);

    checkCompletionVariants(file, "index", "shoppingCart", "zzz");
  }

  public void testCompletionBuilderMethods() {
    configureByController("""
                            class CccController {
                              def shoppingCartFlow = {
                                getBooks {
                                  <caret>
                                }
                              }
                            
                              def zzz = {}
                            }
                            """);

    checkCompletion("on", "action");
  }

  public void testMarkUsagesOfFlowScope() {
    addController("""
                    class CccController {
                      def shoppingCartFlow = {
                        getBooks {
                          def x = flow<caret>.xxx
                        }
                        ggg {
                          def x = flow.yyy
                        }
                      }
                    
                      def fooFlow = {
                        getBooks {
                          def x = flow.xxx + flow.yyy
                        }
                      }
                    }
                    """);
    RangeHighlighter[] res = myFixture.testHighlightUsages("grails-app/controllers/CccController.groovy");
    UsefulTestCase.assertSize(2, res);
  }

  public void testMarkUsagesOfConversationScope() {
    addController("""
                    class CccController {
                      def shoppingCartFlow = {
                        getBooks {
                          def x = conversation<caret>.xxx
                        }
                        ggg {
                          def x = conversation.yyy
                        }
                      }
                    
                      def fooFlow = {
                        getBooks {
                          def x = conversation.xxx + conversation.yyy
                        }
                      }
                    }
                    """);
    RangeHighlighter[] res = myFixture.testHighlightUsages("grails-app/controllers/CccController.groovy");
    UsefulTestCase.assertSize(2, res);
  }

  public void testThrowingEvents() {
    PsiFile file = addController("""
                                   class CccController {
                                   
                                     def rrr() {
                                       return 1;
                                     }
                                   
                                     def zzzFlow = {
                                       xxx {
                                         action {
                                           dddd()
                                           fdskfnsdkfls()
                                         }
                                   
                                         on("dasd") {
                                           asdasdfdfsgdfg()
                                           rrr().byteValue()
                                         }
                                         wwww()
                                       }
                                     }
                                   }
                                   """);
    GrailsTestCase.checkResolve(file, "wwww");
  }

  public void testStateNameReference() {
    PsiFile file = addController("""
                                   class CccController {
                                     def zzzFlow = {
                                       xxx {
                                         action {
                                           success()
                                         }
                                         on("success").to("<caret>")
                                       }
                                       yyy {
                                       }
                                       endState()
                                     }
                                   }
                                   """);
    checkCompletionVariants(file, "xxx", "yyy", "endState");
  }

  public void testActionCompletion() {
    addController("""
                    class VvvController {
                      def index = {}
                      def www = {}
                    }
                    """);

    configureByController("""
                            class CccController {
                              def zzzFlow = {
                                xxx {
                                  action {
                            
                                  }
                                  on("success") {
                                    redirect(controller: "vvv", action: "<caret>")
                                  }
                                }
                              }
                            }
                            """);

    checkCompletion("index", "www");
  }

  public void testControllerCompletion() {
    addController("""
                    class VvvController {
                      def index = {}
                      def www = {}
                    }
                    """);

    configureByController("""
                            class CccController {
                              def zzzFlow = {
                                xxx {
                                  action {
                            
                                  }
                                  on("success") {
                                    subflow(controller: "<caret>")
                                  }
                                }
                              }
                            }
                            """);

    checkCompletion("ccc", "vvv");
  }

  public void testViewGutters() {
    addView("ccc/zzz/xxx.gsp", "");

    PsiFile file = addController("""
                                   class CccController {
                                     def zzzFlow = {
                                       xx<caret>x {
                                         action {
                                   
                                         }
                                         on("success") {
                                           subflow(controller: "")
                                         }
                                       }
                                     }
                                   }
                                   """);

    GutterMark res = myFixture.findGutter(getFilePath(file));
    TestCase.assertNotNull(res);
  }

  @Override
  protected boolean needWebFlow() {
    return true;
  }
}

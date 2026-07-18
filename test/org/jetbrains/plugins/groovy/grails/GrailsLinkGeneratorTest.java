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

package org.jetbrains.plugins.groovy.grails;

import com.intellij.psi.PsiFile;

public class GrailsLinkGeneratorTest extends Grails14TestCase {
  public void testCompletion() {
    addController("""
                    
                    class CccController {
                      def index = {}
                      def xxx = {}
                      def yyy = {}
                    }
                    """);

    PsiFile file = addService("""
                                
                                class XxxService {
                                  org.codehaus.groovy.grails.web.mapping.LinkGenerator grailsLinkGenerator
                                
                                  def foo() {
                                    grailsLinkGenerator.link([controller: "ccc", action: '<caret>'])
                                  }
                                }
                                """);
    checkCompletionVariants(file, "index", "xxx", "yyy");
  }

  public void testActionInConditionalOperator() {
    addController("""
                    
                    class CccController {
                      def index = {}
                      def xxx = {}
                      def yyy = {}
                    }
                    """);

    PsiFile file = addService("""
                                
                                class XxxService {
                                  org.codehaus.groovy.grails.web.mapping.LinkGenerator grailsLinkGenerator
                                
                                  def foo() {
                                    grailsLinkGenerator.link([controller: "ccc", action: b ? '<caret>'])
                                  }
                                }
                                """);
    checkCompletionVariants(file, "index", "xxx", "yyy");
  }

  public void testCompletionContextPath() {
    configureByController("""
                            
                            class CccController {
                              org.codehaus.groovy.grails.web.mapping.LinkGenerator grailsLinkGenerator
                            
                              def index() {
                                grailsLinkGenerator.resource(contextPath: "/<caret>")
                              }
                            }
                            """);

    checkCompletion("grails-app", "src");
  }

  public void testCompletionFile() {
    myFixture.addFileToProject("web-app/css/a.css", "");
    myFixture.addFileToProject("web-app/css/b.css", "");

    configureByController("""
                            
                            class CccController {
                              org.codehaus.groovy.grails.web.mapping.LinkGenerator grailsLinkGenerator
                            
                              def index() {
                                grailsLinkGenerator.resource(dir: 'css', file: "<caret>")
                              }
                            }
                            """);

    checkCompletion("a.css", "b.css");
  }

  public void testConditional() {
    myFixture.addFileToProject("web-app/css/a.css", "");
    myFixture.addFileToProject("web-app/css/b.css", "");

    configureByController("""
                            
                            class CccController {
                              org.codehaus.groovy.grails.web.mapping.LinkGenerator grailsLinkGenerator
                            
                              def index() {
                                grailsLinkGenerator.resource(dir: 'css', file: f ? "<caret>" : "zzz.css")
                              }
                            }
                            """);

    checkCompletion("a.css", "b.css");
  }
}

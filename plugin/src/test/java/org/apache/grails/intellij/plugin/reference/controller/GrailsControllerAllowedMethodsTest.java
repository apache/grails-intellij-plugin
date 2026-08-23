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

package org.apache.grails.intellij.plugin.reference.controller;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.plugin.references.controller.ControllerAllowedMethodReferenceProvider;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GrailsControllerAllowedMethodsTest extends GrailsTestCase {
  public void testCompletionAction1() {
    configureByController("""
                            class CccController {
                              def index1 = {}
                              def index2 = {}
                            
                              static allowedMethods = [<caret>]
                            }
                            """);

    checkCompletion("index1", "index2");
  }

  public void testCompletionAction2() {
    configureByController("""
                            class CccController {
                              def index1 = {}
                              def index2 = {}
                            
                              static allowedMethods = [index<caret>: 'GET']
                            }
                            """);

    checkCompletion("index1", "index2");
  }

  public void testRenameAction() {
    configureByController("""
                            class CccController {
                              def index<caret> = {}
                            
                              static allowedMethods = [index: 'GET']
                            }
                            """);

    myFixture.renameElementAtCaret("xxx");

    myFixture.checkResult("""
                            class CccController {
                              def xxx = {}
                            
                              static allowedMethods = [xxx: 'GET']
                            }
                            """);
  }

  public void testCompletionValue() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {}
                                   
                                     static allowedMethods = [index: '<caret>']
                                   }
                                   """);

    myFixture.testCompletionVariants(getFilePath(file), ControllerAllowedMethodReferenceProvider.HTTP_METHODS);
  }

  public void testCompletionValueList() {
    configureByController("""
                            class CccController {
                              def index = {}
                            
                              static allowedMethods = [index: ['<caret>', 'GET', "POST"]]
                            }
                            """);
    checkCompletion("DELETE");
    checkNonExistingCompletionVariants("GET", "POST");
  }
}

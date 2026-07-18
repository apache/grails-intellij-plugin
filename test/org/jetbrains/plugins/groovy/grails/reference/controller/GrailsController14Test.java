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

package org.jetbrains.plugins.groovy.grails.reference.controller;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMethod;
import org.jetbrains.plugins.groovy.grails.Grails14TestCase;

public class GrailsController14Test extends Grails14TestCase {
  public void testResolve() {
    configureByController("""
                            class CccController {
                              def index = {
                                header<caret>("headerName", "headerValue")
                              }
                            }
                            """);

    PsiElement e = myFixture.getElementAtCaret();
    assertInstanceOf(e, PsiMethod.class);

    assertEquals("org.codehaus.groovy.grails.plugins.web.api.ControllersApi", ((PsiMethod)e).getContainingClass().getQualifiedName());
  }

  public void testNonObjectMethod() {
    configureByController("""
                            class CccController {
                              def index = {
                                setGspEncodin<caret>
                              }
                            }
                            """);

    LookupElement[] res = myFixture.completeBasic();

    assertNotNull(res);
    assertEmpty(res);
  }

  public void testCompletion() {
    PsiFile controller = addController("""
                                         class CccController {
                                           def index = {
                                             <caret>
                                           }
                                         }
                                         """);
    checkCompletion(controller, "log", "render", "modelAndView", "webRequest", "actionName");
  }
}

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

package org.jetbrains.plugins.groovy.grails.reference.taglib;

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsTagLibNamedArgumentsTest extends GrailsTestCase {
  public void testCompletion() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       link(<caret>)
                                     }
                                   }
                                   """);

    checkCompletion(file, "controller", "uri", "url", "ondblclick");
  }

  public void testResolve() {
    configureByController("""
                            class CccController {
                              def index = {
                                link(controlle<caret>r: 'ccc')
                              }
                            }
                            """);

    TestCase.assertNotNull(myFixture.getElementAtCaret());
  }

  public void testCustomTag() {
    addTaglib("""
                class MyTagLib {
                  def xxx = {attr ->
                    out << attr.aaa << attr.bbb
                  }
                }
                """);

    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       xxx(<caret>)
                                     }
                                   }
                                   """);

    checkCompletion(file, "aaa", "bbb");
  }
}

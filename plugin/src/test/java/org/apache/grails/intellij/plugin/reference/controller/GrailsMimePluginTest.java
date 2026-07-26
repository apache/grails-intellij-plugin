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
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GrailsMimePluginTest extends GrailsTestCase {
  public void testResolve() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       withFormat {
                                         html {}
                                         js {}
                                         xml bookList: books
                                         foo(bookList: books)
                                         unresolved(1, 2)
                                       }
                                     }
                                   }
                                   """);
    GrailsTestCase.checkResolve(file, "books", "books", "unresolved");
  }

  public void testWithFormatReturnType() {
    PsiFile file = addController("""
                                   class CccController {
                                     def index = {
                                       withFormat {
                                       }.<caret>
                                     }
                                   }
                                   """);
    checkCompletion(file, "size", "putAll", "containsKey");
  }
}

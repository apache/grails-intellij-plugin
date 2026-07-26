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

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsControllerRenameTest extends GrailsTestCase {
  public void testRename() {
    addController("""
                    class ZzzController {
                      def aaa = {}
                    }
                    """);

    configureByController("""
                            class CccController {
                              def index = {
                                link(controller: 'zzz', action: 'aaa')
                                link(controller: 'ccc<caret>')
                              }
                            }
                            """);

    myFixture.renameElementAtCaret("CcController");

    myFixture.checkResult("""
                            class CcController {
                              def index = {
                                link(controller: 'zzz', action: 'aaa')
                                link(controller: 'cc<caret>')
                              }
                            }
                            """);
  }
}

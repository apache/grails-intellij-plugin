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

package org.apache.grails.intellij.plugin.domain;

import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

public class NamedArgumentsInFindOrCreateMethodTest extends Grails14TestCase {
  public void testCompletion() {
    addDomain("""
                
                class Ddd {
                  String name;
                  String zzz;
                  int iii;
                }
                """);
    configureByController("""
                            
                            class CccController {
                              def index = {
                                Ddd.findOrCreateWhere(name: "Name", <caret>)
                              }
                            }
                            """);

    checkCompletion("zzz", "iii");
    checkNonExistingCompletionVariants("name");
  }

  public void testRename() {
    addDomain("""
                
                class Ddd {
                  String name;
                }
                """);
    configureByController("""
                            
                            class CccController {
                              def index = {
                                Ddd.findOrCreateWhere(name<caret>: "Name")
                              }
                            }
                            """);

    myFixture.renameElementAtCaret("firstName");

    myFixture.checkResult("""
                            
                            class CccController {
                              def index = {
                                Ddd.findOrCreateWhere(firstName: "Name")
                              }
                            }
                            """);
  }
}

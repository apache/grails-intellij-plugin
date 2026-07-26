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

package org.apache.grails.intellij.plugin.inspections;

import org.jetbrains.plugins.groovy.codeInspection.declaration.GrMethodMayBeStaticInspection;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

public class GrailsMethodMayBeStaticInspectionTest extends Grails14TestCase {
  public void testInspectionDisabled() {
    myFixture.enableInspections(GrMethodMayBeStaticInspection.class);
    configureByController("""
                            class CccController {
                                def index() {
                                  render("test")
                                }
                            
                                private foo() {
                                  render("test")
                                }
                            
                                private <warning>bar</warning>() {
                                  def z = 12
                                }
                            }
                            """);
    myFixture.checkHighlighting(true, false, true);
  }
}

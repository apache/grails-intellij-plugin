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

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import org.apache.grails.intellij.plugin.references.controller.ControllerMembersProvider;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

public class GrailsControllerAmbiguousMethodInspectionTest extends Grails14TestCase {
  public void testAmbiguousInspection() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    configureByController("""
                            class CccController {
                              def foo() {
                                render(text: "text")
                              }
                            }
                            """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testResolve() {
    configureByController("""
                            class CccController {
                              def foo() {
                                render<caret>(text: "text")
                              }
                            }
                            """);

    PsiElement res = myFixture.getElementAtCaret();
    assertInstanceOf(res, PsiMethod.class);
    assertEquals(ControllerMembersProvider.CONTROLLER_API_CLASS, ((PsiMethod)res).getContainingClass().getQualifiedName());
  }
}

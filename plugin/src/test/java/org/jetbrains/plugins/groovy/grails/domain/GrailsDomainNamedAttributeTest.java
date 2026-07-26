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

package org.jetbrains.plugins.groovy.grails.domain;

import com.intellij.psi.PsiFile;
import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsDomainNamedAttributeTest extends GrailsTestCase {
  @Override
  protected boolean needGormLibrary() {
    return true;
  }

  public void testCompletionListOrderBy() {
    PsiFile file = addDomain("""
                               
                               class Street {
                                 String name
                               
                                 static {
                                   Street.listOrderByName(<caret>)
                                 }
                               }
                               """);
    checkCompletion(file, "max", "offset", "sort", "fetch");
  }

  public void testHighlightListOrderBy() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    PsiFile file = addDomain("""
                               
                               class Street {
                                 String name
                               
                                 static {
                                   Street.listOrderByName(max: '1', order: <warning descr="Type of argument 'order' can not be 'Boolean'">true</warning>, ignoreCase: <warning descr="Type of argument 'ignoreCase' can not be 'String'">'Yes'</warning>)
                                 }
                               }
                               """);
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }

  public void testCompletionList() {
    PsiFile file = addDomain("""
                               
                               class Street {
                                 String name
                               
                                 static {
                                   def c = Street.createCriteria()
                                   def res = c.list(<caret>) {}
                                 }
                               }
                               """);
    checkCompletion(file, "max", "offset", "sort", "fetch");
  }

  public void testHighlightList() {
    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);

    PsiFile file = addDomain("""
                               
                               class Street {
                                 String name
                               
                                 static {
                                   def c = Street.createCriteria()
                                   def res = c.list(max: '1', order: <warning descr="Type of argument 'order' can not be 'Boolean'">true</warning>, ignoreCase: <warning descr="Type of argument 'ignoreCase' can not be 'String'">'Yes'</warning>) {}
                                 }
                               
                                 static constraints = {
                                   def ss = "aaa$dd 32"
                                   name(matches: <warning descr="Type of argument 'matches' can not be 'GString'">ss</warning>)
                                 }
                               }
                               """);
    myFixture.testHighlighting(true, false, true, file.getVirtualFile());
  }
}

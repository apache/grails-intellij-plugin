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

package org.jetbrains.plugins.groovy.grails.validation;

import org.jetbrains.plugins.groovy.codeInspection.assignment.GroovyAssignabilityCheckInspection;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GrailsImportFromTest extends GrailsTestCase {
  public void testCompletionImportFromDomain() {
    configureByDomain("""
                        class Ddd {
                            String name
                        
                            static constraints = {
                              <caret>
                            }
                        }
                        """);

    checkCompletion("name()", "importFrom()");
  }

  private void addValidateableAnnotationClass() {
    myFixture.addClass("""
                         package grails.validation;
                         @interface Validateable {}
                         """);
  }

  public void testCompletionImportFromValidatableObject() {
    addValidateableAnnotationClass();

    configureBySimpleGroovyFile("""
                                  import grails.validation.Validateable
                                  
                                  @Validateable
                                  class Aaa {
                                      String name
                                  
                                      static constraints = {
                                        <caret>
                                      }
                                  }
                                  """);

    checkCompletion("name", "importFrom()");
  }

  public void testImportFromHighlighting() {
    addValidateableAnnotationClass();

    myFixture.enableInspections(GroovyAssignabilityCheckInspection.class);
    myFixture.enableInspections(GrUnresolvedAccessInspection.class);

    addSimpleGroovyFile("""
                          import grails.validation.Validateable
                          @Validateable
                          class Foo {
                          }
                          """);

    configureBySimpleGroovyFile("""
                                  import grails.validation.Validateable
                                  @Validateable
                                  class Aaa {
                                      String name
                                  
                                      static constraints = {
                                        importFrom(Foo)
                                        importFrom(Foo, include: ['a'])
                                        importFrom(Foo, include: <warning>777</warning>)
                                        importFrom<warning>(12)</warning>
                                        <warning>importFrom123</warning>(12)
                                  
                                        name(notEqual: '_')
                                      }
                                  }
                                  """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testImportFromNamedArgumentsCompletion() {
    addValidateableAnnotationClass();

    addSimpleGroovyFile("""
                          import grails.validation.Validateable
                          @Validateable
                          class Foo {
                          }
                          """);

    configureBySimpleGroovyFile("""
                                  import grails.validation.Validateable
                                  @Validateable
                                  class Aaa {
                                      String name
                                  
                                      static constraints = {
                                        importFrom(Foo, <caret>)
                                      }
                                  }
                                  """);

    checkCompletion("include:", "exclude:");
  }
}

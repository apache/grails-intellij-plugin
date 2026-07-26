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

import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

/**
 * This test tests case of issue IDEA-69797 (http://youtrack.jetbrains.net/issue/IDEA-69797)
 */
public class GrailsRenameDomainPropertyTest extends GrailsTestCase {
  public void testRenameDomainProperty() throws Exception {
    PsiFile domainClass = addDomain("""
                                      
                                      class Ddd {
                                        String name;
                                      
                                        public boolean isUnnamed() {
                                          return name == null;
                                        }
                                      }
                                      """);

    configureBySimpleGroovyFile("new Ddd().isUnnamed<caret>()");

    myFixture.renameElementAtCaret("isEmptyName");

    TestCase.assertEquals("""
                            
                            class Ddd {
                              String name;
                            
                              public boolean isEmptyName() {
                                return name == null;
                              }
                            }
                            """, domainClass.getText());
  }
}

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

package org.apache.grails.intellij.plugin.domain;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GormFetchModeTest extends GrailsTestCase {
  public void testCompletion() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 static <caret>
                               
                               }
                               """);

    checkCompletion(file, "fetchMode");
  }

  public void testCompletionFieldName1() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                  String name;
                               
                                 static hasMany = [mirrors: Mirror]
                                 static fetchMode = [<caret>: 'eager']
                               }
                               """);

    checkCompletion(file, "mirrors");
    checkNonExistingCompletionVariants("name", "id");
  }

  public void testCompletionFieldName2() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String name;
                                 List mirrors;
                               
                                 static hasMany = [mirrors: Mirror]
                                 static fetchMode = [<caret>, ]
                               }
                               """);

    checkCompletion(file, "mirrors");
    checkNonExistingCompletionVariants("name", "id");
  }

  public void testCompletionFieldName3() {
    PsiFile file = addDomain("""
                               
                               class Ddd {
                                 String name;
                               
                                 static hasMany = [mirrors: Mirror]
                                 static fetchMode = [xxx: <caret>]
                               }
                               """);

    checkCompletion(file);
    checkNonExistingCompletionVariants("mirrors");
  }

  public void testRename() {
    configureByDomain("""
                        
                        class DownloadFile {
                          String title
                          List mirrors
                          Download download
                        
                          static hasMany = [mirrors: Mirror]
                          static fetchMode = [mirrors<caret>: 'eager']
                        }
                        """);

    myFixture.renameElementAtCaret("mmm");

    myFixture.checkResult("""
                            
                            class DownloadFile {
                              String title
                              List mmm
                              Download download
                            
                              static hasMany = [mmm: Mirror]
                              static fetchMode = [mmm: 'eager']
                            }
                            """);
  }
}

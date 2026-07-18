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

import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

public class GormUniqueConstraintValueTest extends GrailsTestCase {
  public void testCompletion1() throws Exception {
    addSimpleGroovyFile("""
                          
                          class Zzz {
                            int iii;
                          }
                          """);

    configureByDomain("""
                        
                        class Ddd extends Zzz {
                          String name;
                          String title;
                        
                          static constraints = {
                            name(unique: '<caret>')
                          }
                        }
                        """);

    checkCompletion("title", "iii");
    checkNonExistingCompletionVariants("name", "id", "version");
  }

  public void testCompletion2() {
    configureByDomain("""
                        
                        class Ddd {
                          String name;
                          String title;
                          String zzz;
                          Ddd ddd
                        
                          static constraints = {
                            name(unique: ['<caret>', 'zzz'])
                          }
                        }
                        """);

    checkCompletion("title", "ddd");
    checkNonExistingCompletionVariants("name", "zzz");
  }

  public void testCompletion3() {
    configureByDomain("""
                        
                        class Ddd {
                          String name;
                          String title;
                        
                          static hasMany = [many: String]
                          static hasOne = [one: Ddd]
                        
                          def transientProperty
                        
                          static constraints = {
                            name([unique: ['<caret>', 'zzz']])
                          }
                        }
                        """);

    checkCompletion("title", "one");
    checkNonExistingCompletionVariants("name", "zzz", "transientProperty", "many");
  }

  public void testRename() {
    configureByDomain("""
                        
                        class Ddd {
                          String name;
                          String title<caret>;
                        
                          static constraints = {
                            name([unique: 'title'])
                          }
                        }
                        """);

    myFixture.renameElementAtCaret("ttt");

    myFixture.checkResult("""
                            
                            class Ddd {
                              String name;
                              String ttt;
                            
                              static constraints = {
                                name([unique: 'ttt'])
                              }
                            }
                            """);
  }
}

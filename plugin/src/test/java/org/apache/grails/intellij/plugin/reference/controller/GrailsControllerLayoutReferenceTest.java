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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.util.List;

public class GrailsControllerLayoutReferenceTest extends GrailsTestCase {
  public void testCompletion() {
    addView("layouts/ttt/main.gsp", "");
    addView("layouts/aaa.gsp", "");

    configureByController("""
                            class CccController {
                              static layou<caret>
                            }
                            """);

    LookupElement[] c1 = myFixture.completeBasic();
    assertNull(c1);

    myFixture.completeBasic();
    List<String> res = myFixture.getLookupElementStrings();
    assertSameElements(res, "aaa", "ttt");
  }

  public void testMove() {
    addView("layouts/ttt/fff/main.gsp", "");

    PsiFile ccc = addController("""
                                  class CccController {
                                    static layout = 'ttt/fff/main'
                                  }
                                  """);

    myFixture.moveFile("grails-app/views/layouts/ttt/fff/main.gsp", "grails-app/views/layouts/ttt");

    assertEquals("""
                   class CccController {
                     static layout = 'ttt/main'
                   }
                   """, ccc.getText());
  }
}

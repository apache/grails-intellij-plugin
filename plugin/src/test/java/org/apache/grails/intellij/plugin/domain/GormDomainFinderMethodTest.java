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

import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GormDomainFinderMethodTest extends GrailsTestCase {
  public void testCompletionMethod() throws Exception {
    addDomain("""
                
                class Product {
                
                  String name
                  String date
                  String quality
                  String weight
                
                  public String getSize() {
                    return "asda"
                  }
                
                  public void setSize(String size) {
                
                  }
                
                  int transientField1;
                  String transientField2;
                
                  static transients = ['transientField1', "transientField2"]
                }
                """);

    configureBySimpleGroovyFile("Product.findAllByDateAndNameIlik<caret>");
    assertTrue(myFixture.completeBasic().length >= 1);
    myFixture.type("\n");

    myFixture.checkResult("Product.findAllByDateAndNameIlike(<caret>)");
  }
}

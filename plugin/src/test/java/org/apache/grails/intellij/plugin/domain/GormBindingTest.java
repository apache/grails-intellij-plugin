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

public class GormBindingTest extends GrailsTestCase {
  private void addDomain() {
    addDomain("""
                  class City {
                    String name;
                
                    public int getPopulation() {
                      return 1;
                    }
                  }
                """);
  }

  public void testResolve() {
    if (!useGrails14()) {
      myFixture.addClass("""
        package org.codehaus.groovy.grails.web.servlet.mvc;
        public class GrailsParameterMap implements java.util.Map {}
      """);
    }

    addDomain();
    PsiFile c = addController("""
      class CccController {
        def save = {
          def b = City.get(params.id)
          b.properties = params
          b.save()
          b.unresolvedReference = 2;
        }
      }
    """);
    GrailsTestCase.checkResolve(c, "unresolvedReference");
  }

  public void testCompletion() {
    addDomain();
    PsiFile file = addController("""
                                   class CccController {
                                     def save = {
                                       def b = City.get(params.id)
                                       b.properties['<caret>', ""\"name""\"] = params
                                       b.save()
                                     }
                                   }
                                   """);

    checkCompletionVariants(file, "population", "id", "version");
  }

  public void testRename() {
    addDomain();

    configureByController("""
                            
                            class CccController {
                              def save = {
                                def b = City.get(params.id)
                                b.properties['populatio<caret>n', ""\"name""\"] = params
                                b.properties['population', ""\"name""\"] = params
                                b.save()
                              }
                            }
                            """);

    myFixture.renameElementAtCaret("getPpp");

    myFixture.checkResult("""
                            
                            class CccController {
                              def save = {
                                def b = City.get(params.id)
                                b.properties['ppp', ""\"name""\"] = params
                                b.properties['ppp', ""\"name""\"] = params
                                b.save()
                              }
                            }
                            """);
  }
}

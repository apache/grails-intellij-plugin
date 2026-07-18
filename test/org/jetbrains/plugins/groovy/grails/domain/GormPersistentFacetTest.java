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

import com.intellij.facet.FacetManager;
import com.intellij.hibernate.facet.HibernateFacet;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.HddGrailsTestCase;

public class GormPersistentFacetTest extends HddGrailsTestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myFixture.addFileToProject("grails-app/conf/hibernate/hibernate.cfg.xml", "<hibernate-configuration></hibernate-configuration>");
    setupFacets();
  }

  public void testAddFacet() {
    HibernateFacet facet = FacetManager.getInstance(getModule()).getFacetByType(HibernateFacet.ID);
    TestCase.assertNotNull(facet);
    UsefulTestCase.assertSize(1, facet.getDescriptors());
  }

  public void testCompletionInFindAll() {
    addDomain("""
                
                class Ddd {
                  String name;
                  static hasMany = [many: String]
                }
                """);

    PsiFile file = addController("""
                                   
                                   class CccController {
                                     def index = {
                                       Ddd.findAll("from Ddd d where d.<caret>")
                                     }
                                   }
                                   """);
    checkCompletion(file, "id", "version", "many", "name");
  }

  public void testCompletionInExecuteQuery() {
    addDomain("""
                
                class Ddd {
                  String name;
                  static hasMany = [many: String]
                }
                """);

    PsiFile file = addController("""
                                   
                                   class CccController {
                                     def index = {
                                       Ddd.executeQuery("from Ddd d where d.<caret>")
                                     }
                                   }
                                   """);
    checkCompletion(file, "id", "version", "many", "name");
  }

  public void testCompletionInFind() {
    addDomain("""
                
                class Ddd {
                  String name;
                  static hasMany = [many: String]
                }
                """);

    PsiFile file = addController("""
                                   
                                   class CccController {
                                     def index = {
                                       Ddd.find("from Ddd d where d.<caret>")
                                     }
                                   }
                                   """);
    checkCompletion(file, "id", "version", "many", "name");
  }
}

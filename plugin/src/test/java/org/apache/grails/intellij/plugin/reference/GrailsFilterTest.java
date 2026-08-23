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

package org.apache.grails.intellij.plugin.reference;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.io.IOException;
import java.io.UncheckedIOException;

public class GrailsFilterTest extends GrailsTestCase {
  @Override
  protected void configureModule(Module module, ModifiableRootModel model, ContentEntry contentEntry) {
    TempDirTestFixture tdf = myFixture.getTempDirFixture();
    VirtualFile file;
    try {
      file = tdf.findOrCreateDir("grails-app/conf");
    }
    catch (IOException e) {
      throw new UncheckedIOException(e);
    }
    contentEntry.addSourceFolder(file, false);
  }

  public void testResolve() {
    PsiFile filter1 = myFixture.addFileToProject("grails-app/conf/zzz/eee/GggFilters.groovy", """
      package zzz.eee;
      class GggFilters {
        def all() {
      
        }
      
        def filters = {
           if (true) {
             correctFilter1 controller:"*", action: "*", {}
             error1(controller:"*", action: "*", {}) {}
             error2(controller:"*", 1, action: "*") {}
             error3(controller:"*", action: "*")
             error4({}, [controller:"*", action: "*"])
             correctFilter2([controller:"*", action: "*"]) {}
             correctFilter3([controller:"*", action: "*"], {})
             correctFilter4() {}
      
             all(controller:"*", action: "*") {
               if (1 > 2) {
                 before = {
                   someMethod()
                 }
               }
               else {
                 before = {}
               }
             }
           }
        }
      }
      """);

    PsiFile filter2 = myFixture.addFileToProject("grails-app/conf/GggFilters.groovy", """
      class GggFilters {
        static filters = {
           all(controller:"*", action: "*") {
             before = {
               someMethod()
             }
           }
        }
      }
      """);

    GrailsTestCase.checkResolve(filter1, "error1", "error2", "error3", "error4", "someMethod");
    GrailsTestCase.checkResolve(filter2, "someMethod");
  }

  public void testActionRename() {
    PsiFile www = configureByController("class WwwController {def waction<caret> = {} }");

    PsiFile filter = myFixture.addFileToProject("grails-app/conf/FffFilters.groovy", """
      class FffFilters {
        def filters = {
          all(controller: '*', action: 'waction') {
            before = {
              redirect(controller: 'www', action: 'waction')
            }
          }
        }
      }
      """);

    myFixture.renameElementAtCaret("bbb");

    assertEquals("class WwwController {def bbb = {} }", www.getText());

    assertEquals("grails-app/conf/FffFilters.groovy", """
      class FffFilters {
        def filters = {
          all(controller: '*', action: 'bbb') {
            before = {
              redirect(controller: 'www', action: 'bbb')
            }
          }
        }
      }
      """, filter.getText());
  }

  public void testControllerRename() {
    PsiFile www = myFixture.addFileToProject("grails-app/controllers/WwwController.groovy", "class WwwController<caret> {def waction={} }");
    PsiFile filter = myFixture.addFileToProject("grails-app/conf/FffFilters.groovy", """
      class FffFilters {
        def filters = {
          all(controller: 'www', action: '*') {
            before = {
              render(controller: 'www', action: 'waction')
            }
          }
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(www.getVirtualFile());

    myFixture.renameElementAtCaret("BbbController");

    assertEquals("class BbbController {def waction={} }", www.getText());
    assertEquals("grails-app/conf/FffFilters.groovy", """
      class FffFilters {
        def filters = {
          all(controller: 'bbb', action: '*') {
            before = {
              render(controller: 'bbb', action: 'waction')
            }
          }
        }
      }
      """, filter.getText());
  }

  public void testPropertyFirst() {
    PsiFile filter = myFixture.addFileToProject("grails-app/conf/FffFilters.groovy", """
      class FffFilters {
        def bbb(Map m, Closure c) {}
      
        def filters = {
          aaa(controller: '*', action: '*') {
            before = {
      
            }
          }
          int xxx = 10;
          xxx(controller: '*', action: '*') {
            after = {
      
            }
          }
          bbb(controller: '*', action: '*') {
            afterView = {
            }
          }
        }
      }
      """);

    GrailsTestCase.checkResolve(filter, "after", "afterView");
  }
}

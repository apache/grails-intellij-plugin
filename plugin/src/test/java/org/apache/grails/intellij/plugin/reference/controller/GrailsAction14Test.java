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

package org.apache.grails.intellij.plugin.reference.controller;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

public class GrailsAction14Test extends Grails14TestCase {
  public void testCompletion() {
    myFixture.addFileToProject("grails-app/controllers/SuperClass.groovy", """
      class SuperClass {
      
        def index = {}
      
        def yyy(int x, String s) {
      
        }
      
        def getCcc() {
          return {
            render "aaa"
          }
        }
      
        def getYyy() {
          render "aaa"
        }
      
      }
      """);

    PsiFile file = addController("""
                                   class CccController extends SuperClass {
                                     def text = {
                                       redirect controller : 'ccc', action: '<caret>'
                                     }
                                   
                                     def kkk = aaaa;
                                   
                                   
                                     public void xxx() {
                                       render("zzz")
                                     }
                                   
                                     Closure getZzz() {
                                       return {
                                         render("zzz")
                                       }
                                     }
                                   }
                                   """);

    checkCompletionVariants(file, "text", "xxx", "getZzz", "getYyy", "getCcc", "yyy", "index");
  }

  public void testRename() {
    configureByController("""
                            class CccController {
                              def getData<caret>() {
                              }
                            }
                            """);
    PsiFile file = addView("ccc/getData.gsp", "");

    myFixture.renameElementAtCaret("getFooData");

    assertEquals("getFooData.gsp", file.getName());
  }
}

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

package org.jetbrains.plugins.groovy.grails.config;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import junit.framework.TestCase;
import org.jetbrains.plugins.groovy.grails.GrailsTestCase;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class GrailsConfigFileTest extends GrailsTestCase {
  private void doTest(String text, List<String> exists, List<String> notExists) {
    configureByBuildConfig(text);

    LookupElement[] res = myFixture.completeBasic();

    HashSet<String> variants = new HashSet<>(exists);

    for (LookupElement r : res) {
      String s = r.getLookupString();
      TestCase.assertFalse(s, notExists.contains(s));
      variants.remove(s);
    }


    UsefulTestCase.assertEmpty(variants);
  }

  public void testCompletion1() {
    doTest("""
             grails.views.default.codec = "none"
             <caret>
             """, List.of("grails.mime.file.extensions", "grails.mime.use.accept.header", "grails.mime.types"),
           List.of("grails.views.default.codec"));
  }

  public void testCompletion2() {
    doTest("""
             grails {
                 views.default.codec = "aaa"
                 <caret>
             }
             """, List.of("mime.file.extensions", "mime.use.accept.header", "mime.types"),
           List.of("views.default.codec"));
  }

  public void testCompletion3() {
    doTest("""
             grails {
                 views.default.codec = projec<caret>
             }
             """, Collections.emptyList(),
           List.of("views.default.codec", "mime.file.extensions", "mime.use.accept.header"));
  }

  //public void testCompletion4() {
  //  doTest("""
  //           grails {
  //               mime."file".extensions = "aaa"
  //               mi<caret> = "bbb"
  //           }
  //           """,
  //         List.of("mime.use.accept.header", "mime.types"),
  //         List.of("mime.file.extensions", "mime"));
  //}


  public void testCompletion5() {
    configureByBuildConfig("""
                             grails {
                                 views.default.codec = "aaa"
                                 exceptionresolver.logRequestParamete<caret> = "bbb"
                             }
                             """);
    LookupElement[] res = myFixture.completeBasic();
    TestCase.assertNull(res);

    myFixture.checkResult("""
                            grails {
                                views.default.codec = "aaa"
                                exceptionresolver.logRequestParameters =<caret> "bbb"
                            }
                            """);
  }

  public void testCompletion6() {
    doTest("""  
             grails {
                 views.default.codec = "aaa"
                 exceptionresolver.logRequestParamete = "bbb"
             }
             
             <caret>
             """, Collections.emptyList(), List.of("grails.serverURL", "grails.views.gsp.sitemesh.preprocess"));// All properties started with 'grails' should be hidden.
  }

  public void testEnvironment() {
    configureByBuildConfig("""
                             environments {
                               development {
                                 grails {
                                   views.default.codec = "aaa"
                                   exceptionresolver.logRequestParamete<caret> = "bbb"
                                 }
                               }
                             }
                             """);
    LookupElement[] res = myFixture.completeBasic();
    TestCase.assertNull(res);

    myFixture.checkResult("""
                            environments {
                              development {
                                grails {
                                  views.default.codec = "aaa"
                                  exceptionresolver.logRequestParameters =<caret> "bbb"
                                }
                              }
                            }
                            """);
  }

  private PsiFile addBuildConfig(String text) {
    return myFixture.addFileToProject("grails-app/conf/Config.groovy", text);
  }

  private void configureByBuildConfig(String text) {
    PsiFile file = addBuildConfig(text);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
  }
}

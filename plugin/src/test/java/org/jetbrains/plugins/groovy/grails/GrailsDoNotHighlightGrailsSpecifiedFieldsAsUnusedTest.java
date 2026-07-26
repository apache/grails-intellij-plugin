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

package org.jetbrains.plugins.groovy.grails;

import com.intellij.codeInspection.deadCode.UnusedDeclarationInspectionBase;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.plugins.groovy.codeInspection.GroovyUnusedDeclarationInspection;

public class GrailsDoNotHighlightGrailsSpecifiedFieldsAsUnusedTest extends Grails14TestCase {
  public void testHighlightingController() {
    myFixture.enableInspections(new GroovyUnusedDeclarationInspection(), new UnusedDeclarationInspectionBase(true));

    configureByController("""
                            class CccController {
                              static defaultAction = "index"
                              def beforeInterceptor = null
                              static <warning>foo</warning>
                              def bar
                              def <warning>unused</warning>
                              def index = {
                                render(bar.toString())
                              }
                              def zzz() {
                              }
                            }
                            """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testHighlightingDomain() {
    myFixture.enableInspections(new GroovyUnusedDeclarationInspection(), new UnusedDeclarationInspectionBase(true));

    configureByDomain("""
                        class Ddd {
                          static <warning>defaultAction</warning> = "index"
                          def onLoad() {
                          }
                          def <warning>zzz</warning>() {
                          }
                        }
                        """);

    myFixture.checkHighlighting(true, false, true);
  }

  public void testHighlightingFieldsInBootstrap() {
    PsiTestUtil.addSourceRoot(myFixture.getModule(), myFixture.getTempDirFixture().getFile("grails-app/conf"));
    try {
      myFixture.enableInspections(new GroovyUnusedDeclarationInspection(), new UnusedDeclarationInspectionBase(true));

      PsiFile bootstrapFile = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
          class BootStrap {
              def init = { servletContext ->
                bitcoinService.start()
              }
              def destroy = {
                bitcoinService.stop()
              }
              def <warning>foo</warning> = {
              }
          }
        """);
      myFixture.configureFromExistingVirtualFile(bootstrapFile.getVirtualFile());

      myFixture.checkHighlighting(true, false, true);
    }
    finally {
      PsiTestUtil.removeSourceRoot(myFixture.getModule(), myFixture.getTempDirFixture().getFile("grails-app/conf"));
    }
  }

  public void testHighlightingFieldsInCodec() {
    myFixture.enableInspections(new GroovyUnusedDeclarationInspection(), new UnusedDeclarationInspectionBase(true));

    PsiFile bootstrapFile = myFixture.addFileToProject("grails-app/utils/MyCodec.groovy", """
      class MyCodec {
       static encode = { theTarget ->
          theTarget.toString()
       }
       static decode = { theTarget ->
          theTarget.toString()
       }
      
       static <warning>foo</warning> = 10;
      }
      """);

    PsiTestUtil.addSourceRoot(myFixture.getModule(), myFixture.getTempDirFixture().getFile("grails-app/utils"));

    try {
      myFixture.configureFromExistingVirtualFile(bootstrapFile.getVirtualFile());

      myFixture.checkHighlighting(true, false, true);
    }
    finally {
      PsiTestUtil.removeSourceRoot(myFixture.getModule(), myFixture.getTempDirFixture().getFile("grails-app/utils"));
    }
  }
}

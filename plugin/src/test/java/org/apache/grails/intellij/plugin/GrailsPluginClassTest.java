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

package org.apache.grails.intellij.plugin;


import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import junit.framework.TestCase;
import org.apache.grails.intellij.plugin.GrailsPluginFieldCompletionProvider;

public class GrailsPluginClassTest extends GrailsTestCase {
  public void testUniqueCompletionVariants() {
    TestCase.assertEquals(GrailsPluginFieldCompletionProvider.VARIANTS.length,
                          ContainerUtil.newHashSet(GrailsPluginFieldCompletionProvider.VARIANTS).size());
  }

  public void testCompletion() {
    myFixture.addFileToProject("XxxGrailsPlugin.groovy", """
      class XxxGrailsPlugin {
        def doWith<caret>
      }
      """);
    myFixture.testCompletionVariants("XxxGrailsPlugin.groovy", "doWithApplicationContext", "doWithDynamicMethods", "doWithSpring");
  }

  public void testCompletion1() {
    PsiFile file = myFixture.addFileToProject("XxxGrailsPlugin.groovy", """
      class XxxGrailsPlugin {
        def <caret>
      }
      """);
    checkCompletion(file, "doWithApplicationContext", "doWithDynamicMethods", "doWithSpring");
  }

  public void testCompletionStatic() {
    myFixture.addFileToProject("XxxGrailsPlugin.groovy", """
      class XxxGrailsPlugin {
        static def doWith<caret>
      }
      """);
    myFixture.testCompletionVariants("XxxGrailsPlugin.groovy", "doWithApplicationContext", "doWithDynamicMethods", "doWithSpring");
  }

  public void testCompletionStatic1() {
    PsiFile file = myFixture.addFileToProject("XxxGrailsPlugin.groovy", """
      class XxxGrailsPlugin {
        static def <caret>
      }
      """);
    checkCompletion(file, "doWithApplicationContext", "doWithDynamicMethods", "doWithSpring");
  }

  public void testRenamePluginExcludes() {
    myFixture.addFileToProject("scripts/ppp/aaa.groovy", "");

    PsiFile file = myFixture.addFileToProject("CccGrailsPlugin.groovy", """
      class CccGrailsPlugin {
        def pluginExcludes = ['scripts/ppp/**', 'scripts/ppp/aaa.groovy']
      }
      """);

    myFixture.moveFile("scripts/ppp/aaa.groovy", "scripts");

    TestCase.assertEquals("""
                            class CccGrailsPlugin {
                              def pluginExcludes = ['scripts/ppp/**', 'scripts/aaa.groovy']
                            }
                            """, file.getText());
  }

  public void testCompletionObserveValues() {
    myFixture.addFileToProject("Aaa1GrailsPlugin.groovy", "class Aaa1GrailsPlugin {}");
    myFixture.addFileToProject("Aaa2GrailsPlugin.groovy", "class Aaa2GrailsPlugin {}");
    myFixture.addFileToProject("Aaa3GrailsPlugin.groovy", "class Aaa3GrailsPlugin {}");
    myFixture.addFileToProject("Aaa0GrailsPlugin.groovy", """
      class Aaa2GrailsPlugin {
          def observe = ['aaa1', 'aaa<caret>']
      }
      """);

    myFixture.testCompletionVariants("Aaa0GrailsPlugin.groovy", "aaa2", "aaa3");
  }

  public void testClosureArgumentTypes() {
    PsiFile file = myFixture.addFileToProject("XxxGrailsPlugin.groovy", """
      class XxxGrailsPlugin {
          def onChange = {event ->
              event.containsKey(null)
              event.dfsdkfsdjk()
          }
      }
      """);

    GrailsTestCase.checkResolve(file);
  }
}

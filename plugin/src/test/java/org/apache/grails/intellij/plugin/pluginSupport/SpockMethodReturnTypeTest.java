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

package org.apache.grails.intellij.plugin.pluginSupport;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ContentEntry;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestUtil;

public class SpockMethodReturnTypeTest extends GrailsTestCase {
  @Override
  protected void configureGrails(@NotNull Module module, @NotNull ModifiableRootModel model, ContentEntry contentEntry) {
    super.configureGrails(module, model, contentEntry);
    PsiTestUtil.addLibrary(model, "Spoc", GrailsTestUtil.getMockGrails11LibraryHome(), "/lib/spock-grails-support-0.5-groovy-1.7.jar");
  }

  public void testControllerTestCompletion() {
    addController("class CccController { def zzz = {} }");

    PsiFile file = myFixture.addFileToProject("test/unit/CccControllerSpec.groovy", """
      class CccControllerSpec extends grails.plugin.spock.ControllerSpec {
        def "test"() {
          controllerClass.newInstance().<caret>
        }
      }
      """);

    checkCompletion(file, "zzz", "session", "request");
  }

  public void testTagLibTestCompletion() {
    addTaglib("class MyTagLib { def zzz = {} }");

    PsiFile file = myFixture.addFileToProject("test/unit/MyTagLibSpec.groovy", """
      class MyTagLibSpec extends grails.plugin.spock.TagLibSpec {
        def "test"() {
          tagLibClass.newInstance().<caret>
        }
      }
      """);

    checkCompletion(file, "zzz");
  }
}

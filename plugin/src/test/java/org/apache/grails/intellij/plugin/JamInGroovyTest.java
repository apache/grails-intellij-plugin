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

import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.annotations.NotNull;
import org.junit.Assert;

public class JamInGroovyTest extends LightJavaCodeInsightFixtureTestCase {
  private static final LightProjectDescriptor SPRING_PROJECT =
    new DefaultLightProjectDescriptor().withRepositoryLibrary("org.springframework:spring-beans:4.3.21.RELEASE"
    ).withRepositoryLibrary("org.springframework:spring-core:4.3.21.RELEASE");

  @Override
  protected @NotNull LightProjectDescriptor getProjectDescriptor() {
    return SPRING_PROJECT;
  }

  public void testJamReference() {
    myFixture.addClass("""
                         package org.springframework.test.context;
                         public @interface ContextConfiguration {
                           String locations();
                         }""");
    PsiFile xml = myFixture.addFileToProject("foo/bar.xml", "");
    myFixture.configureByText("Foo.groovy", """
      @org.springframework.test.context.ContextConfiguration(locations="classpath:/foo/ba<caret>r.xml"
      class Foo {}
      """);
    PsiReference ref = myFixture.getFile().findReferenceAt(myFixture.getEditor().getCaretModel().getOffset());
    Assert.assertEquals(ref.resolve(), xml);
  }
}

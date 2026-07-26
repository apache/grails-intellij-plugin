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

package org.jetbrains.plugins.groovy.grails.gsp;

import com.intellij.lang.injection.InjectedLanguageManager;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.psi.ElementManipulator;
import com.intellij.psi.ElementManipulators;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import junit.framework.TestCase;
import org.jetbrains.plugins.grails.lang.gsp.GspLanguage;
import org.jetbrains.plugins.grails.lang.gsp.psi.groovy.impl.GspOuterHtmlElementImpl;

public class GspElementManipulatorForOuterLanguageElementTest extends LightJavaCodeInsightFixtureTestCase {
  public void testContentChange() {
    PsiFile file = myFixture.configureByText("a.gsp", """
      <g:javascript>
          <caret>var a = 1
      </g:javascript>
      """);

    final PsiElement element = InjectedLanguageManager.getInstance(getProject()).getTopLevelFile(file).getViewProvider()
      .findElementAt(InjectedLanguageUtil.getTopLevelEditor(myFixture.getEditor()).getCaretModel().getOffset(), GspLanguage.INSTANCE);
    UsefulTestCase.assertInstanceOf(element, GspOuterHtmlElementImpl.class);

    final ElementManipulator<PsiElement> manipulator = ElementManipulators.getManipulator(element);
    TestCase.assertNotNull(manipulator);

    WriteCommandAction.runWriteCommandAction(getProject(), (Runnable)() -> manipulator.handleContentChange(element, "aaa"));

    myFixture.checkResult("""
                            <g:javascript>aaa</g:javascript>
                            """);
  }
}

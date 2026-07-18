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

package org.jetbrains.plugins.groovy.grails.parser;

import com.intellij.lang.Language;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.DebugUtil;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.jetbrains.plugins.groovy.util.TestUtils;

public abstract class GspParsingTestCase extends LightJavaCodeInsightFixtureTestCase {

  protected void doTest(Language lang) {
    final String path = getTestName(true).replace('$', '/') + ".test";
    final String input = TestUtils.readInput(getTestDataPath() + path).get(0);

    final PsiFile file = TestUtils.createPseudoPhysicalFile(getProject(), "temp.gsp", input);
    final PsiFile psi = file.getViewProvider().getPsi(lang);
    final String actualPsiText = DebugUtil.psiToString(psi, true).trim();

    myFixture.configureByText("test.txt", input + "\n-----\n" + actualPsiText);
    myFixture.checkResultByFile(path, false);
  }
}

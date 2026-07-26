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

package org.apache.grails.intellij.plugin.completion;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.UsefulTestCase;
import com.intellij.testFramework.fixtures.CompletionAutoPopupTestCase;

public class GrailsTestAutoCompletionHardReferencesTest extends CompletionAutoPopupTestCase {
  public void testCompletion() {
    myFixture.addFileToProject("folder1/a.txt", "");
    myFixture.addFileToProject("folder2/a.txt", "");
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BuildConfig.groovy", """
      grails.project.class.dir = "<caret>"\s""");

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    type("f");

    UsefulTestCase.assertSameElements(myFixture.getLookupElementStrings(), "folder1", "folder2");
  }
}

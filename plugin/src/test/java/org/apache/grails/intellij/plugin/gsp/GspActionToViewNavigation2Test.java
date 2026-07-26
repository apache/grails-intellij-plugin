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

package org.apache.grails.intellij.plugin.gsp;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

public class GspActionToViewNavigation2Test extends Grails14TestCase {
  public void testGuttersExists() {
    myFixture.addFileToProject("grails-app/views/ccc/index.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/xxx.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/zzz.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/notAAction.gsp", "");
    myFixture.addFileToProject("grails-app/views/ccc/notAAction2.gsp", "");

    PsiFile file = addController(
      """
        class CccController {
          def index = {
        
          }
        
          def xxx() {
          }
        
          def zzz(String param1, String param2) {
          }
        
          def notAAction;
        
          public static def getNotAAction2() {
            return 23
          }
        
        }
        """);
    checkGutters(file, "index", "xxx", "zzz");
  }
}

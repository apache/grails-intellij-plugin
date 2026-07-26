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

package org.apache.grails.intellij.plugin.domain;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

public class GormApiFromJavaTest extends Grails14TestCase {
  public void testCompletionStatic() {
    addDomain("""
                
                class Ddd {
                  String name;
                }
                """);

    PsiFile file = myFixture.addFileToProject("src/java/Jjj.java", """
      
      public class Jjj {
        static {
          Ddd.<caret>
        }
      }
      """);

    checkCompletion(file, "create", "saveAll", "get");
    checkNonExistingCompletionVariants("createQueryMapForExample", "getId", "hasErrors", "setTransactionManager", "getExtendedMethods");
  }

  public void testCompletion() {
    addDomain("""
                
                class Ddd {
                  String name;
                }
                """);

    PsiFile file = myFixture.addFileToProject("src/java/Jjj.java", """
      
      public class Jjj {
        static {
          new Ddd().<caret>
        }
      }
      """);

    checkCompletion(file, "refresh", "getName", "clearErrors", "hasErrors", "validate");
    checkNonExistingCompletionVariants("create", "getErrors", "filterErrors", "setTransactionManager", "getExtendedMethods");
  }

  public void testHighlighting() {
    addDomain("""
                
                class Ddd {
                  String name;
                }
                """);

    PsiFile file = myFixture.addFileToProject("src/java/Jjj.java", """
      
      public class Jjj {
        static {
          Ddd.saveAll(new Ddd(), new Ddd(), new Ddd());
          Ddd.saveAll(new Ddd[]{new Ddd(), new Ddd(), new Ddd()});
      
          java.util.List list = Ddd.getAll();
      
          new Ddd().refresh();
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }
}

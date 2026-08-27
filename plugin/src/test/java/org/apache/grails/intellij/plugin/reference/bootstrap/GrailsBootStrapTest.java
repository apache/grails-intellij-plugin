/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.grails.intellij.plugin.reference.bootstrap;

import com.intellij.psi.PsiFile;
import com.intellij.testFramework.PsiTestUtil;
import org.jetbrains.plugins.groovy.codeInspection.untypedUnresolvedAccess.GrUnresolvedAccessInspection;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

public class GrailsBootStrapTest extends Grails14TestCase {
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    PsiTestUtil.addSourceRoot(myFixture.getModule(), myFixture.getTempDirFixture().getFile("grails-app/conf"));
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      PsiTestUtil.removeSourceRoot(myFixture.getModule(), myFixture.getTempDirFixture().getFile("grails-app/conf"));
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testCompletion() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      
      class BootStrap {
      
          def init = { servletContext ->
            <caret>
          }
      }
      """);
    checkCompletion(file, "environments");
  }

  public void testCompletion2() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      
      class BootStrap {
      
          def init = { servletContext ->
            environments {
              <caret>
            }
          }
      }
      """);
    checkCompletion(file);
    checkNonExistingCompletionVariants("environments");
  }

  public void testCompletionEnvName() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      
      class BootStrap {
      
          def init = { servletContext ->
            environments({
              <caret>
            })
          }
      }
      """);
    checkCompletion(file, "test", "development", "production");
  }

  public void testCompletionEnvNameNotCompleted() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      
      class BootStrap {
      
          def init = { servletContext ->
            environments({
              if (1 == 2) {
                <caret>
              }
            })
          }
      }
      """);
    checkCompletion(file);
    checkNonExistingCompletionVariants("test", "development", "production");
  }

  public void testResolve() {
    myFixture.enableInspections(GrUnresolvedAccessInspection.class);
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      class BootStrap {
          def init = { servletContext ->
            String.<warning>environments</warning> {
              <warning>test</warning> {
              }
      
            }
      
            environments {
              if (1 == 2) {
                <warning>test</warning> {
                }
              }
      
              development({
              })
      
              production {
      
              }
      
              <warning>environments</warning> {
              }
            }
      
            environments ({
            });
          }
      
          def foo() {
           <warning>environments</warning> {
           }
          }
      }
      """);

    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting(true, false, true);
  }

  public void testLogVariable() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/BootStrap.groovy", """
      class BootStrap {
        def init = { servletContext ->
          <caret>
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    checkCompletion("log");
  }
}

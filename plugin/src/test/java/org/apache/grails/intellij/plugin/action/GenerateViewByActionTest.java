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

package org.apache.grails.intellij.plugin.action;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.psi.PsiFile;
import junit.framework.TestCase;
import org.intellij.lang.annotations.Language;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.lib.testFramework.HddGrailsTestCase;

public class GenerateViewByActionTest extends HddGrailsTestCase {
  private void testAction(@Language("devkit-action-id") String actionId, boolean enabled) {
    AnAction action = ActionManager.getInstance().getAction(actionId);
    Presentation presentation = myFixture.testAction(action);
    TestCase.assertEquals(enabled, presentation.isEnabled());
    TestCase.assertEquals(enabled, presentation.isVisible());
  }

  public void testGenerateView1() {
    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def index = {
          <caret>
        }
      }
      """);
    myFixture.configureFromExistingVirtualFile(controllerFile.getVirtualFile());
    testAction("Generate.GrailsView", true);
    TestCase.assertNotNull(myFixture.findFileInTempDir("grails-app/views/ccc/index.gsp"));
  }

  public void testViewAlreadyExists() {
    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def index = {
          <caret>
        }
      }
      """);
    myFixture.addFileToProject("grails-app/views/ccc/index.gsp", "");
    myFixture.configureFromExistingVirtualFile(controllerFile.getVirtualFile());
    testAction("Generate.GrailsView", false);
  }

  public void testNotAnAction1() {
    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def index = {
          return new Runnable() {
            def zzz = {
              <caret>
            }
          }
        }
      }
      """);
    myFixture.addFileToProject("grails-app/views/ccc/index.gsp", "");
    myFixture.configureFromExistingVirtualFile(controllerFile.getVirtualFile());
    testAction("Generate.GrailsView", false);
  }

  public void testNotAnAction2() {
    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        public int getZzz() {
          <caret>
        }
      }
      """);

    myFixture.configureFromExistingVirtualFile(controllerFile.getVirtualFile());

    testAction("Generate.GrailsView", false);
  }

  public void testIntention2() {
    PsiFile controllerFile = myFixture.addFileToProject("grails-app/controllers/CccController.groovy", """
      class CccController {
        def index<caret> = {
      
        }
      }
      """);

    runIntention(controllerFile, GrailsBundle.message("intention.text.create.view.gsp.page"), true);
    TestCase.assertNotNull(myFixture.findFileInTempDir("grails-app/views/ccc/index.gsp"));
  }
}

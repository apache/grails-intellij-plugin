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

package org.apache.grails.intellij.plugin.config;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GrailsConfigSlurperTest extends GrailsTestCase {
  public void testCompletionAfterMethodCall() {
    PsiFile file = addView("a.gsp", "${grailsApplication.getConfig().<caret>}");
    checkCompletion(file, "grails.project.groupId", "environments");
  }

  public void testCompletionAfterReference() {
    PsiFile file = addView("a.gsp", "${grailsApplication.config.<caret>}");
    checkCompletion(file, "grails.project.groupId", "environments");
  }

  public void testCompletionInner() {
    PsiFile file = addView("a.gsp", "${grailsApplication.config.grails.mime.<caret>}");
    checkCompletion(file, "file.extensions", "types", "use.accept.header");
  }

  public void testConfigurationHolder() throws Exception {
    configureBySimpleGroovyFile("org.codehaus.groovy.grails.commons.ConfigurationHolder.getConfig().environments.<caret>");
    checkCompletion("development", "test", "production");
  }

  public void testCompletionByDot() {
    configureByView("a.gsp", "${grailsApplication.getConfig().<caret>}");
    myFixture.completeBasic();
    myFixture.type("grai.pro.g=");
    myFixture.checkResult("${grailsApplication.getConfig().grails.project.groupId=}");
  }
}

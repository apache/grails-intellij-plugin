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

package org.apache.grails.intellij.plugin.config;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

public class GrailsConstraintGroupTest extends Grails14TestCase {
  public void testCompletion() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      grails.gorm.default.constraints = {
          '*'(nullable:true, blank:false, size:1..20, <caret>)
      }
      """);

    checkCompletion(file, "creditCard", "inList", "min");
    checkNonExistingCompletionVariants("blank", "nullable");
  }

  public void testHighlighting() {
    PsiFile file = myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      grails.gorm.default.constraints = {
          '*'(nullable:true, blank:"true", size: new Object())
      }
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());

    myFixture.checkHighlighting(true, false, true);
  }

  public void testCompletionShared() {
    myFixture.addFileToProject("grails-app/conf/Config.groovy", """
      grails.gorm.default.constraints = {
          '*'(nullable:true, blank:false, size:1..20)
          aaa1(nullable:true)
          aaa2(nullable:false)
      }
      """);

    PsiFile d = addDomain("""
                            class Ddd {
                              String name;
                              static constraints = {
                                name(shared: "<caret>")
                              }
                            }
                            """);

    checkCompletionVariants(d, "aaa1", "aaa2");
  }
}

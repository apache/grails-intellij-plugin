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

package org.apache.grails.intellij.plugin.gsp;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GspAttributeFromJavadocTest extends GrailsTestCase {
  public void testCompletion() {
    addTaglib("""
                class MyTagLib {
                
                  /**
                   * @attr aaa sdsda
                   * @attr bbb sdsda
                   */
                  def xxx = { attr ->
                    out << attr.xxx + attr.aaa
                  }
                }
                """);

    PsiFile file = addView("a.gsp", "<g:xxx <caret> />");
    myFixture.testCompletionVariants(getFilePath(file), "xxx", "aaa", "bbb");
  }

  public void testRequiredAttribute() {
    addTaglib("""
                class MyTagLib {
                
                  /**
                   * @attr aaa required sdsda
                   * @attr bbb REQUIRED sdsda
                   * @attr ccc
                   */
                  def xxx = { attr, body ->
                    out << body << attr
                  }
                }
                """);

    configureByView("a.gsp", "<g:xx<caret>");
    myFixture.completeBasic();
    myFixture.checkResult("<g:xxx aaa=\"\" bbb=\"\"");
  }
}

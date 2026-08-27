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
import org.jetbrains.plugins.groovy.GroovyLanguage;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GspPropertyResolvePriorityTest extends GrailsTestCase {
  /**
   * Test: g:each Variables > ModelVariables > DefaultVariables, Taglibs
   */
  public void testResolvePriority1() {
    addController(
      """
        class CccController {
          def index = {
            [exception:"string"]
          }
        }
        """);
    addTaglib(
      """
        class MyTagLib {
          def exception = {
            out << "exception"
          }
        }
        """);
    addTaglib(
      """
        class MyWithNamespaceTagLib {
          static namespace='exception'
          def exception = {
            out << "exception"
          }
        }
        """);
    PsiFile gspFile = myFixture.addFileToProject("grails-app/views/ccc/index.gsp", """
      <% out << exception.substring(1) %>
      
      <g:each var="exception" in="[1,2,3]">
        ${exception.byteValue()}<br/>
      </g:each>
      """);
    GrailsTestCase.checkResolve(gspFile.getViewProvider().getPsi(GroovyLanguage.INSTANCE));
  }
}

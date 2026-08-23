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

import org.apache.grails.intellij.plugin.fileType.GspFileType;
import com.intellij.testFramework.LightProjectDescriptor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.GroovyProjectDescriptors;
import org.jetbrains.plugins.groovy.lang.highlighting.GrHighlightingTestBase;

public class GspHighlightingTest extends GrHighlightingTestBase {
  @Override
  protected @NotNull LightProjectDescriptor getProjectDescriptor() {
    return GroovyProjectDescriptors.MOCK_JDK_11;
  }

  public void testGroovyInsideGsp() {
    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, """
      <%@ page contentType="text/html;charset=UTF-8"%>
      <html>
      <head>
          <title></title>
      </head>
      <body>
      <%
          def <info descr="null" textAttributesKey="Groovy var">a</info>
          def <info descr="null" textAttributesKey="Groovy var">x</info>
          def <info descr="null" textAttributesKey="Groovy var">c</info> = <info descr="null" textAttributesKey="Groovy var">a</info> + <info descr="null" textAttributesKey="Groovy var">x</info>
          if (true) {
              <info descr="null" textAttributesKey="Instance field">out</info>.println <info descr="null" textAttributesKey="Groovy var">c</info>
          }
      %>
      </body>
      </html>
      """);
    myFixture.testHighlighting(false, true, true);
  }
}

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

public class GspNamedMappingTest extends GrailsTestCase {
  private void addMappingFile() {
    myFixture.addFileToProject("grails-app/conf/UrlMappings.groovy", """
      class UrlMappings {
      
        static mappings = {
          name mapping111: "/m1/$param1/${param2}" {
            controller = 'ccc'
            action = 'zzz'
          }
      
          name mappingWithOptionalParams: "/m1/$param1?/${param2}?" {
            controller = 'ccc'
            action = 'zzz'
          }
      
          name (mapping222: "/m1/dsddsad" {
            controller = 'ccc'
            action = 'zzz'
          })
      
          "/"(view: "/index")
          "500"(view: '/error')
        }
      }
      """);
  }

  public void testHighlighting() {
    addMappingFile();
    PsiFile file = myFixture.addFileToProject("grails-app/views/g.gsp", """
      <link:mapping111 param1="asd" param2="asdasd" zzz="sda">Link</link:mapping111>
      <link:mapping222>L</link:mapping222>
      <link:mappingWithOptionalParams>L</link:mappingWithOptionalParams>
      <link:mappingWithOptionalParams param1="1" param2="2">LLL</link:mappingWithOptionalParams>
      
      <<error descr="Element link:noMapping is not allowed here">link:noMapping</error>>Link</<error descr="Element link:noMapping is not allowed here">link:noMapping</error>>
      
      <g:link mapping="mapping222">Link</g:link>
      <g:link mapping="<error descr="Cannot resolve symbol 'NoMapping'">NoMapping</error>">Link</g:link>
      """);
    myFixture.configureFromExistingVirtualFile(file.getVirtualFile());
    myFixture.checkHighlighting();
  }

  public void testAttributeCompletion() {
    addMappingFile();
    PsiFile file = myFixture.addFileToProject("grails-app/views/g.gsp", "<link:mapping111 <caret>");
    checkCompletionVariants(file, "attrs", "param1", "param2");
  }
}

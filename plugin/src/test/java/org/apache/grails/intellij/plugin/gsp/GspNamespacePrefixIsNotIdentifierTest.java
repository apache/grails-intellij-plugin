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

public class GspNamespacePrefixIsNotIdentifierTest extends GrailsTestCase {
  public void testNamespacePrefixIsNotIdentifier() {
    addTaglib(
      """
        class MyTagLib {
        
          static namespace = "import"
        
          def xxx = { }
          def yyy = { }
        
        }
        """);

    PsiFile b = myFixture.addFileToProject("grails-app/views/b.gsp", "<import:<caret>");
    checkCompletionVariants(b, "xxx", "yyy");
  }

  public void testAccessViaThis() {
    addTaglib(
      """
        class MyTagLib {
        
          static namespace = "import"
        
          def xxx = { }
          def yyy = { }
        
        }
        """);

    PsiFile gsp = addView("a.gsp", "${this.'import'.<caret>}");
    checkCompletion(gsp, "xxx", "yyy");
  }
}

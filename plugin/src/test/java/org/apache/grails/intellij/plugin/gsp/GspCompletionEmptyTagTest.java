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

import com.intellij.codeInsight.lookup.LookupElement;
import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

public class GspCompletionEmptyTagTest extends GrailsTestCase {
  public void testCompletionEmptyTag() {
    addTaglib("""
                class MyTagLib {
                
                  def tagWithoutBody = {attr ->
                  }
                
                }
                """);
    configureByView("a.gsp", "<g:tagWithout<caret>");
    LookupElement[] res = myFixture.completeBasic();
    TestCase.assertNull(res);

    myFixture.checkResult("<g:tagWithoutBody/><caret>");
  }

  public void testEmptyTagJavadoc() {
    addTaglib("""
                class MyTagLib {
                
                  /**
                   * @emptyTag
                   */
                  def xxx = { attr, body ->
                
                  }
                }
                """);

    configureByView("a.gsp", "<g:xx<caret>");
    myFixture.completeBasic();

    myFixture.checkResult("<g:xxx/><caret>");
  }
}

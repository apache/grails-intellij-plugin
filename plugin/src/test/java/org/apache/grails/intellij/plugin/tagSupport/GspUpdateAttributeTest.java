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

package org.apache.grails.intellij.plugin.tagSupport;

import com.intellij.psi.PsiFile;
import org.apache.grails.intellij.lib.testFramework.Grails14TestCase;

public class GspUpdateAttributeTest extends Grails14TestCase {
  public void testXmlAttr() {
    PsiFile view = addView("a.gsp", """
      <div id="ddd"> </div>
      <g:link id="lll">sss</g:link>
      
      <g:remoteLink update="<caret>">s</g:remoteLink>
      """);

    checkCompletionVariants(view, "ddd", "lll");
  }

  public void testGroovyAttr() {
    PsiFile view = addView("a.gsp", """
      <div id="ddd"> </div>
      <g:link id="lll">sss</g:link>
      
      ${remoteLink(update: '<caret>')}
      
      """);

    checkCompletionVariants(view, "ddd", "lll");
  }
}

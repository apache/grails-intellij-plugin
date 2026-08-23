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

import junit.framework.TestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.util.List;

public class GspCodecAttributeTest extends GrailsTestCase {
  public void testCodecHighlighting() {
    myFixture.addFileToProject("grails-app/utils/MmmCodec.groovy", """
      class MmmCodec {
       public static decode(Object target) { return "s"; }
      }
      """);

    myFixture.addFileToProject("grails-app/utils/NnnCodec.groovy", """
      class NnnCodec {
       public static encode(Object target) { return "s"; }
       public static decode(Object target) { return "s"; }
      }
      """);

    myFixture.addFileToProject("grails-app/views/a.gsp", """
      <g:message encodeAs="<error descr="Cannot resolve symbol 'Mmm'">Mmm</error>" />
      <g:message encodeAs="HTML" />
      <g:message encodeAs="SHA1" />
      <g:message encodeAs="Nnn" />
      """);

    myFixture.addFileToProject("grails-app/views/b.gsp", "<g:message encodeAs='<caret>' />");

    List<String> v = myFixture.getCompletionVariants("grails-app/views/b.gsp");
    TestCase.assertTrue(v.contains("Nnn"));
    TestCase.assertFalse(v.contains("Mmm"));
  }
}

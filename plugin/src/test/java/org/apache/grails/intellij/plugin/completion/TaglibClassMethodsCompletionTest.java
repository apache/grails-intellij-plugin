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

package org.apache.grails.intellij.plugin.completion;

import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.util.List;

import static org.apache.grails.intellij.lib.testFramework.GrailsTestUtil.getTestRootPath;

public class TaglibClassMethodsCompletionTest extends GrailsTestCase {
  public void testTaglibCompletion() {
    configureByTaglib("""
                        class MyTagLib {
                          def customTag = {
                            re<caret>
                          }
                        }
                        """);

    myFixture.completeBasic();
    assertTrue(myFixture.getLookupElementStrings().containsAll(
      List.of("remoteField", "remoteFunction", "remoteLink", "render", "renderErrors", "request", "resolveStrategy", "resource",
              "response", "return")));
  }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/oldCompletion/taglib/");
  }

}

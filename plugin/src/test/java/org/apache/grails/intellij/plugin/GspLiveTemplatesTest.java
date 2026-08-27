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
package org.apache.grails.intellij.plugin;

import com.intellij.codeInsight.lookup.Lookup;
import com.intellij.codeInsight.lookup.LookupManager;
import com.intellij.codeInsight.lookup.impl.LookupImpl;
import com.intellij.codeInsight.template.impl.actions.ListTemplatesAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;

public class GspLiveTemplatesTest extends LightJavaCodeInsightFixtureTestCase {

  public void testHtmlTemplatesWorkInGsp() {
    myFixture.configureByText("a.gsp", "c<caret>");
    expandTemplate(myFixture.getEditor());
    myFixture.checkResult("<!-- <caret> -->");
  }

  private static void expandTemplate(final Editor editor) {
    new ListTemplatesAction().actionPerformedImpl(editor.getProject(), editor);
    ((LookupImpl) LookupManager.getActiveLookup(editor)).finishLookup(Lookup.NORMAL_SELECT_CHAR);
  }
}
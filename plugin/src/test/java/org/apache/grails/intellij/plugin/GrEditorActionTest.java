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

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.apache.grails.intellij.plugin.fileType.GspFileType;
import org.jetbrains.plugins.groovy.util.TestUtils;

import java.util.List;

import static org.apache.grails.intellij.lib.testFramework.GrailsTestUtil.getTestRootPath;

public class GrEditorActionTest extends LightJavaCodeInsightFixtureTestCase {

  private void doTest() {
    final List<String> data = TestUtils.readInput(getTestDataPath() + getTestName(true) + ".test");

    myFixture.configureByText(GspFileType.GSP_FILE_TYPE, "");

    final String fileText = data.get(0);

    for (int i = 0; i < fileText.length(); i++) {
      final char charTyped = fileText.charAt(i);
      myFixture.type(charTyped);
    }
    myFixture.checkResult(data.get(1));
  }

  public void testDir() { doTest(); }
  public void testSimple_type() { doTest(); }

  @Override
  protected String getTestDataPath() {
    return getTestRootPath("/testdata/grails/enterAction/");
  }
}

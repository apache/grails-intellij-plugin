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

package org.apache.grails.intellij.plugin.runner;

import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.testFramework.IdeaTestUtil;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

/**
 * The {@code --enable-native-access} launcher flag silences JLine's foreign-memory warnings, but
 * it is an unrecognized option on older JDKs and would prevent the JVM from starting at all, so
 * it must only be added for recent launch JDKs.
 */
public class GrailsCommandLineExecutorTest extends GrailsTestCase {

  public void testNativeAccessAllowedOnRecentJdk() {
    JavaParameters params = new JavaParameters();
    params.setJdk(JavaSdk.getInstance().createJdk("TEST_RECENT_JDK", IdeaTestUtil.requireRealJdkHome(), false));
    GrailsCommandLineExecutor.allowNativeAccess(params);
    assertTrue("the flag must be added for the recent test JDK: " + params.getVMParametersList().getList(),
               params.getVMParametersList().getList().contains("--enable-native-access=ALL-UNNAMED"));
  }

  public void testNativeAccessSkippedOnOldJdk() {
    JavaParameters params = new JavaParameters();
    params.setJdk(IdeaTestUtil.getMockJdk11());
    GrailsCommandLineExecutor.allowNativeAccess(params);
    assertTrue("the flag must not be added for an old JDK, where it would break the launch: "
               + params.getVMParametersList().getList(),
               params.getVMParametersList().getList().isEmpty());
  }

  public void testNativeAccessNotDuplicated() {
    JavaParameters params = new JavaParameters();
    params.setJdk(JavaSdk.getInstance().createJdk("TEST_RECENT_JDK", IdeaTestUtil.requireRealJdkHome(), false));
    params.getVMParametersList().add("--enable-native-access=ALL-UNNAMED");
    GrailsCommandLineExecutor.allowNativeAccess(params);
    assertEquals("a user-supplied flag must not be duplicated",
                 1, params.getVMParametersList().getList().size());
  }
}

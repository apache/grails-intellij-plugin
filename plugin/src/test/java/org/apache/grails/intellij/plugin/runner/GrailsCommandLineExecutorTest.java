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
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.ProjectJdkTable;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.projectRoots.SdkModificator;
import com.intellij.testFramework.IdeaTestUtil;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;

import java.util.List;

/**
 * The {@code --enable-native-access} launcher flag silences JLine's foreign-memory warnings, but
 * JDKs before 17 do not know the option and refuse to start, so it is only added for launch JDKs
 * known to need it (22+). The JDKs below carry a fixed version string, so every case is
 * deterministic whatever JDK the test itself runs on.
 */
public class GrailsCommandLineExecutorTest extends GrailsTestCase {

  private static final String FLAG = "--enable-native-access=ALL-UNNAMED";

  private static Sdk jdkReportingVersion(String versionString) {
    Sdk jdk = ProjectJdkTable.getInstance().createSdk("TEST_JDK_" + versionString, JavaSdk.getInstance());
    SdkModificator modificator = jdk.getSdkModificator();
    modificator.setHomePath(IdeaTestUtil.requireRealJdkHome());
    modificator.setVersionString(versionString);
    WriteAction.run(modificator::commitChanges);
    return jdk;
  }

  private static JavaParameters parametersFor(Sdk jdk) {
    JavaParameters params = new JavaParameters();
    params.setJdk(jdk);
    return params;
  }

  public void testNativeAccessAllowedOnJdk22() {
    JavaParameters params = parametersFor(jdkReportingVersion("22.0.2"));
    GrailsCommandLineExecutor.allowNativeAccess(params);
    assertEquals(List.of(FLAG), params.getVMParametersList().getList());
  }

  public void testNativeAccessAllowedOnLaterJdks() {
    JavaParameters params = parametersFor(jdkReportingVersion("25.0.3"));
    GrailsCommandLineExecutor.allowNativeAccess(params);
    assertEquals(List.of(FLAG), params.getVMParametersList().getList());
  }

  public void testNativeAccessSkippedBelowJdk22() {
    JavaParameters params = parametersFor(jdkReportingVersion("21.0.4"));
    GrailsCommandLineExecutor.allowNativeAccess(params);
    assertEmpty("JLine only needs the flag from JDK 22 on: " + params.getVMParametersList().getList(),
                params.getVMParametersList().getList());
  }

  public void testNativeAccessSkippedOnOldJdk() {
    JavaParameters params = parametersFor(IdeaTestUtil.getMockJdk11());
    GrailsCommandLineExecutor.allowNativeAccess(params);
    assertEmpty("the flag must not be added for an old JDK, where it would break the launch: "
                + params.getVMParametersList().getList(),
                params.getVMParametersList().getList());
  }

  public void testNativeAccessSkippedWithoutAJdk() {
    JavaParameters params = new JavaParameters();
    GrailsCommandLineExecutor.allowNativeAccess(params);
    assertEmpty("an unknown launch JDK must leave the parameters untouched", params.getVMParametersList().getList());
  }

  public void testNativeAccessNotDuplicated() {
    JavaParameters params = parametersFor(jdkReportingVersion("22.0.2"));
    params.getVMParametersList().add(FLAG);
    GrailsCommandLineExecutor.allowNativeAccess(params);
    assertEquals("a user-supplied flag must not be duplicated", List.of(FLAG), params.getVMParametersList().getList());
  }

  public void testUserSuppliedModuleListIsKept() {
    JavaParameters params = parametersFor(jdkReportingVersion("22.0.2"));
    params.getVMParametersList().add("--enable-native-access=org.example.terminal");
    GrailsCommandLineExecutor.allowNativeAccess(params);
    assertEquals("a user's own value of the option wins over ALL-UNNAMED",
                 List.of("--enable-native-access=org.example.terminal"), params.getVMParametersList().getList());
  }
}

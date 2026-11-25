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

import com.intellij.execution.ExecutionException;
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.execution.ParametersListUtil;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;
import org.apache.grails.intellij.plugin.runner.impl.Grails9RunApp;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.GradleGrailsApplications;
import org.jetbrains.plugins.gradle.util.GradleConstants;

import java.util.List;
import java.util.Map;

/**
 * {@code run-app} of a Grails run configuration for a project of Grails 9 is the Gradle task {@code bootRun}, run by the
 * Gradle support of the IDE, with the options of {@code run-app} that have an equivalent given as the command line of
 * Grails 9 gives them to the build.
 */
public class Grails9RunAppTest extends GrailsTestCase {

  private static List<String> gradleArguments(String commandLine) throws ExecutionException {
    return Grails9RunApp.gradleArguments(MvcCommand.parse(commandLine));
  }

  public void testRunAppIsRunInDevelopment() throws ExecutionException {
    assertEquals(List.of("-Dgrails.env=development"), gradleArguments("run-app"));
  }

  public void testTheEnvironmentOfTheShellIsTheGrailsEnvProperty() throws ExecutionException {
    assertEquals(List.of("-Dgrails.env=production"), gradleArguments("prod run-app"));
    assertEquals(List.of("-Dgrails.env=test"), gradleArguments("test run-app"));
    assertEquals(List.of("-Dgrails.env=staging"), gradleArguments("run-app -Dgrails.env=staging"));
    assertEquals(List.of("-Dgrails.env=staging"), gradleArguments("-Dgrails.env=staging run-app"));
  }

  public void testPortAndHostAreTheServerPropertiesOfTheApplication() throws ExecutionException {
    List<String> expected = List.of("-Dgrails.env=development", "-Dgrails.server.port=9090", "-Dgrails.server.address=0.0.0.0");
    assertEquals(expected, gradleArguments("run-app --port 9090 --host 0.0.0.0"));
    assertEquals("the spelling of the shell", expected, gradleArguments("run-app -port 9090 -host 0.0.0.0"));
    assertEquals(expected, gradleArguments("run-app --port=9090 --host=0.0.0.0"));
    assertEquals(expected, gradleArguments("run-app -port=9090 -host=0.0.0.0"));
  }

  public void testPropertiesAndWhatFollowsTheSeparatorGoToGradle() throws ExecutionException {
    assertEquals(List.of("-Dgrails.env=development", "-Dfoo=bar", "-Pmessage=two words", "-D", "baz=qux", "--offline", "--args=--debug"),
                 gradleArguments("run-app -Dfoo=bar \"-Pmessage=two words\" -D baz=qux -- --offline --args=--debug"));
  }

  public void testTheDebuggerOfTheIdeTakesThePlaceOfDebugJvm() throws ExecutionException {
    assertEquals(List.of("-Dgrails.env=development"), gradleArguments("run-app --debug-jvm"));
    assertEquals(List.of("-Dgrails.env=development"), gradleArguments("run-app -debug-jvm"));
  }

  public void testOptionsOnlyTheCommandLineHasAreRefused() {
    for (String option : List.of("--https", "--detach", "--foreground", "--query-tail", "--ready-timeout=10", "--verbose", "--nope")) {
      try {
        gradleArguments("run-app " + option);
        fail(option);
      }
      catch (ExecutionException e) {
        assertTrue(e.getMessage(), e.getMessage().contains(option));
      }
    }
  }

  public void testAMissingValueAndAnArgumentAreRefused() {
    try {
      gradleArguments("run-app --port");
      fail();
    }
    catch (ExecutionException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("--port"));
    }
    try {
      gradleArguments("run-app bootRun");
      fail();
    }
    catch (ExecutionException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("bootRun"));
    }
  }

  public void testTheGradleRunIsBootRunInTheProjectOfTheApplication() throws Exception {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("shop");
    Grails3Application grails9 = GradleGrailsApplications.application(getProject(), root, "9.0.0", List.of());
    MvcCommand command = MvcCommand.parse("run-app --port 9090").setEnvVariables(Map.of("SPRING_PROFILES_ACTIVE", "local")).setPassParentEnvs(false);

    ExternalSystemTaskExecutionSettings settings = Grails9RunApp.createSettings(grails9, command, "Grails: shop");

    assertEquals(GradleConstants.SYSTEM_ID, settings.getExternalSystemId());
    assertEquals(root.getPath(), settings.getExternalProjectPath());
    assertEquals("the helper gives the application no path in its build", List.of("bootRun"), settings.getTaskNames());
    assertEquals(List.of("-Dgrails.env=development", "-Dgrails.server.port=9090"), ParametersListUtil.parse(settings.getScriptParameters()));
    assertEquals(Map.of("SPRING_PROFILES_ACTIVE", "local"), settings.getEnv());
    assertFalse(settings.isPassParentEnvs());
    assertEquals("Grails: shop", settings.getExecutionName());
  }

  /**
   * The name {@code bootRun} alone selects the task of every project below the directory Gradle is run in, so the run
   * of a root application would start the applications below it too. The task is named by the path of the project.
   */
  public void testTheTaskIsTheOneOfTheGradleProjectOfTheApplicationAndOfNoOther() throws Exception {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("shop");
    VirtualFile child = myFixture.getTempDirFixture().findOrCreateDir("shop/admin");
    VirtualFile nested = myFixture.getTempDirFixture().findOrCreateDir("shop/apps/store");

    assertEquals("the root project, also one with projects below it", ":bootRun",
                 Grails9RunApp.taskPath(GradleGrailsApplications.applicationAt(getProject(), root, "9.0.0", ":")));
    assertEquals(":admin:bootRun", Grails9RunApp.taskPath(GradleGrailsApplications.applicationAt(getProject(), child, "9.0.0", ":admin")));
    assertEquals(":apps:store:bootRun",
                 Grails9RunApp.taskPath(GradleGrailsApplications.applicationAt(getProject(), nested, "9.0.0", ":apps:store")));
    assertEquals("a project the import says no path of", "bootRun",
                 Grails9RunApp.taskPath(GradleGrailsApplications.application(getProject(), root, "9.0.0", List.of())));
    assertEquals("what is no path of a project", "bootRun",
                 Grails9RunApp.taskPath(GradleGrailsApplications.applicationAt(getProject(), root, "9.0.0", "")));

    ExternalSystemTaskExecutionSettings ofTheRoot = Grails9RunApp.createSettings(
      GradleGrailsApplications.applicationAt(getProject(), root, "9.0.0", ":"), MvcCommand.parse("run-app"), "Grails: shop");
    assertEquals(root.getPath(), ofTheRoot.getExternalProjectPath());
    assertEquals(List.of(":bootRun"), ofTheRoot.getTaskNames());

    ExternalSystemTaskExecutionSettings ofTheChild = Grails9RunApp.createSettings(
      GradleGrailsApplications.applicationAt(getProject(), child, "9.0.0", ":admin"), MvcCommand.parse("prod run-app"), "Grails: admin");
    assertEquals(child.getPath(), ofTheChild.getExternalProjectPath());
    assertEquals(List.of(":admin:bootRun"), ofTheChild.getTaskNames());
    assertEquals(List.of("-Dgrails.env=production"), ParametersListUtil.parse(ofTheChild.getScriptParameters()));
  }

  public void testVmOptionsAreRefusedSinceTheyWouldNotReachTheApplication() throws Exception {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("shop");
    Grails3Application grails9 = GradleGrailsApplications.application(getProject(), root, "9.0.0", List.of());
    try {
      Grails9RunApp.createSettings(grails9, MvcCommand.parse("run-app").setVmOptions("-Xmx2g"), "Grails: shop");
      fail();
    }
    catch (ExecutionException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("-Xmx2g") && e.getMessage().contains("bootRun"));
    }
  }

  public void testOnlyRunAppIsHandedToGradle() {
    assertTrue(Grails9RunApp.isRunApp(MvcCommand.parse("prod run-app --port 9090")));
    assertFalse(Grails9RunApp.isRunApp(MvcCommand.parse("run-command dbm-status")));
    assertFalse(Grails9RunApp.isRunApp(MvcCommand.parse("test-app")));
  }
}

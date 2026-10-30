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
package org.apache.grails.intellij.plugin.gradle;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectManager;
import org.apache.grails.intellij.lib.testFramework.UltimateOnlyTest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase;
import org.jetbrains.plugins.gradle.tooling.VersionMatcherRule;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * A heavy import test whose Gradle JVM cannot be resolved must fail on its own and leave no project
 * open. When the ubuntu-26.04 runner image dropped JDK 8, the Gradle 4.6 case of
 * {@link GradleGrailsImportingTest} failed inside {@code setUpInWriteAction()}, the platform could not
 * tear the fixture down from under the write action, and the leaked project made
 * {@code GspResourceTagTest.testMove} fail with "Trying to get PSI for a file that is not included in
 * the project model". {@code GradleImportingTestCase.setUpFixtures()} now resolves the JVM before the
 * project exists.
 */
@Category(UltimateOnlyTest.class)
public class GradleImportingTestCaseJdkResolutionTest {

  @Test
  public void unresolvableGradleJvmFailsTheTestWithoutLeakingItsProject() {
    List<Project> openBefore = openProjects();

    Result result;
    MissingJdkProbe.armed = true;
    try {
      result = new JUnitCore().run(MissingJdkProbe.class);
    }
    finally {
      MissingJdkProbe.armed = false;
    }

    assertThat(result.getRunCount()).isEqualTo(1);
    assertThat(result.getFailures()).hasSize(1);
    Throwable failure = result.getFailures().get(0).getException();
    // The resolver's exception must surface as-is. A RuntimeException wrapper means it was thrown
    // inside the write action of NioExternalSystemTestCase.setUp(), where the fixture cannot be
    // torn down and the project leaks.
    assertThat(failure).isExactlyInstanceOf(IllegalStateException.class).hasMessage(MissingJdkProbe.MESSAGE);
    assertThat(openProjects()).containsExactlyInAnyOrderElementsOf(openBefore);
  }

  private static List<Project> openProjects() {
    if (ApplicationManager.getApplication() == null) return Collections.emptyList();
    return List.of(ProjectManager.getInstance().getOpenProjects());
  }

  /**
   * Runs only from the enclosing test. Gradle's class scan picks this class up as well, and it then
   * has no parameters, so nothing runs.
   */
  @RunWith(Parameterized.class)
  @Category(UltimateOnlyTest.class)
  public static class MissingJdkProbe extends GradleImportingTestCase {
    static final String MESSAGE = "No JDK for this probe";
    static volatile boolean armed;

    @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
    @Parameterized.Parameters(name = "with Gradle-{0}")
    public static Collection<Object[]> data() {
      if (!armed) return Collections.emptyList();
      return Collections.singletonList(new Object[]{VersionMatcherRule.BASE_GRADLE_VERSION});
    }

    @Override
    public @NotNull String requireJdkHome() {
      throw new IllegalStateException(MESSAGE);
    }

    @Test
    public void probe() {
      throw new AssertionError("setUp must have failed before the test body ran");
    }
  }
}

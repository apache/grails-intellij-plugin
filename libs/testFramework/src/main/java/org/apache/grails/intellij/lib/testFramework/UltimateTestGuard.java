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
package org.apache.grails.intellij.lib.testFramework;

import org.junit.Assume;
import org.apache.grails.intellij.plugin.util.UltimatePluginGuard;

/**
 * Safe-guard for tests that exercise Ultimate-only integrations. The Community Edition test task
 * excludes the {@link UltimateOnlyTest} category, so these tests should never load there in the
 * first place; this guard is the second line of defense for runs that bypass the filter (for
 * example an IDE-launched single-test run), turning them into skipped tests instead of
 * {@code NoClassDefFoundError}s.
 */
public final class UltimateTestGuard {

  private UltimateTestGuard() {
  }

  /** @return {@code true} when at least one Ultimate-only integration plugin is installed. */
  public static boolean isUltimatePlatform() {
    return UltimatePluginGuard.isPluginAvailable(UltimatePluginGuard.JAVAEE_PLUGIN)
           || UltimatePluginGuard.isPluginAvailable(UltimatePluginGuard.JSP_PLUGIN)
           || UltimatePluginGuard.isPluginAvailable(UltimatePluginGuard.EL_PLUGIN)
           || UltimatePluginGuard.isPluginAvailable(UltimatePluginGuard.SPRING_PLUGIN);
  }

  /**
   * Skips the calling test unless the test sandbox has the Ultimate plugins installed. Called from
   * a {@code @BeforeClass} method of any test tagged {@link UltimateOnlyTest}.
   */
  public static void assumeUltimate() {
    Assume.assumeTrue("Requires the IntelliJ Ultimate plugins on the test sandbox", isUltimatePlatform());
  }
}
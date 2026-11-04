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

package org.apache.grails.intellij.plugin.editor;

import junit.framework.TestCase;
import org.apache.grails.intellij.plugin.util.version.VersionImpl;

import java.util.List;

/**
 * The console no longer refreshes the VFS after a command, so "Generate test" has to refresh the
 * generated spec itself before looking it up; these are the paths it refreshes per Grails layout.
 */
public class GenerateTestsActionTest extends TestCase {

  private static final String ARTEFACT = "com.example.BookController";

  public void testGrails3LayoutUsesTheSourceTestRoots() {
    assertEquals(List.of("src/test/groovy/com/example/BookControllerSpec.groovy"),
                 GenerateTestsAction.expectedTestPaths(new VersionImpl("3.3.1"), ARTEFACT, false));
    assertEquals(List.of("src/integration-test/groovy/com/example/BookControllerSpec.groovy"),
                 GenerateTestsAction.expectedTestPaths(new VersionImpl("7.0.0"), ARTEFACT, true));
  }

  public void testGrails2LayoutCoversTheSpockAndTheJUnitName() {
    assertEquals(List.of("test/unit/com/example/BookControllerSpec.groovy",
                         "test/unit/com/example/BookControllerTests.groovy"),
                 GenerateTestsAction.expectedTestPaths(new VersionImpl("2.5.6"), ARTEFACT, false));
    assertEquals(List.of("test/integration/com/example/BookControllerSpec.groovy",
                         "test/integration/com/example/BookControllerTests.groovy"),
                 GenerateTestsAction.expectedTestPaths(new VersionImpl("2.5.6"), ARTEFACT, true));
  }
}

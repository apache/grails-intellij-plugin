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

package org.apache.grails.intellij.plugin.structure.impl;

import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ProjectKeys;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.vfs.VirtualFile;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.gradle.GrailsModuleData;
import org.apache.grails.intellij.plugin.runner.impl.Grails9RunApp;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.GradleGrailsApplications;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.jetbrains.plugins.gradle.util.GradleModuleDataKt;

import java.util.List;

/**
 * The path of the Gradle project of a Grails application is the one the Gradle import recorded for its module, which
 * is what names the tasks of that project and of no other.
 */
public class Grails3ApplicationGradlePathTest extends GrailsTestCase {

  /** An application as {@link Grails3ApplicationProvider} makes it from the module the Gradle import describes. */
  private Grails3Application application(String directory, String id, @Nullable String gradlePath) throws Exception {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir(directory);
    ModuleData module = new ModuleData(id, GradleConstants.SYSTEM_ID, "JAVA_MODULE", root.getName(), root.getPath(), root.getPath());
    if (gradlePath != null) {
      GradleModuleDataKt.setGradlePath(module, gradlePath);
    }
    DataNode<ModuleData> node = new DataNode<>(ProjectKeys.MODULE, module, null);
    node.createChild(GrailsModuleData.KEY,
                     new GrailsModuleData(GradleConstants.SYSTEM_ID, "9.0.0", GradleGrailsApplications.PLUGIN_ID, List.of(), List.of()));
    return new Grails3MultiModuleApplication(getProject(), root, node);
  }

  public void testTheRootProjectOfABuild() throws Exception {
    Grails3Application root = application("shop", "shop", ":");

    assertEquals(":", root.getGradlePath());
    assertEquals(":bootRun", Grails9RunApp.taskPath(root));
  }

  public void testAProjectBelowTheRoot() throws Exception {
    Grails3Application child = application("shop/admin", ":admin", ":admin");
    Grails3Application nested = application("shop/apps/store", ":apps:store", ":apps:store");

    assertEquals(":admin", child.getGradlePath());
    assertEquals(":admin:bootRun", Grails9RunApp.taskPath(child));
    assertEquals(":apps:store:bootRun", Grails9RunApp.taskPath(nested));
  }

  public void testAModuleTheImportRecordedNoPathOf() throws Exception {
    Grails3Application application = application("shop", "shop", null);

    assertNull(application.getGradlePath());
    assertEquals("the name alone, as before", "bootRun", Grails9RunApp.taskPath(application));
  }
}

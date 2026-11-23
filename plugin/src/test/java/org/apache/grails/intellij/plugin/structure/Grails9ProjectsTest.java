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

package org.apache.grails.intellij.plugin.structure;

import com.intellij.openapi.vfs.VirtualFile;
import org.apache.grails.intellij.lib.gradle.tooling.builder.GrailsCommandLine;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.runner.GrailsCommandExecutor;
import org.apache.grails.intellij.plugin.runner.impl.Grails3GradleCommandExecutor;
import org.apache.grails.intellij.plugin.runner.impl.Grails9CommandExecutor;
import org.apache.grails.intellij.plugin.util.version.VersionImpl;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * Which projects have the command line of Grails 9: the version decides, as {@code grailsVersion} names it, and the
 * {@code .grails/project.yml} record only marks the root of a project.
 */
public class Grails9ProjectsTest extends GrailsTestCase {

  public void testTheMajorVersionOfGrailsVersionDecides() {
    for (String version : List.of("9.0.0", "9.0.0-SNAPSHOT", "9.0.0-M1", "9.0.0-RC1", "9.1.2", "10.0.0", " 9.0.0")) {
      assertTrue(version, GrailsCommandLine.isUsedBy(version));
    }
    for (String version : List.of("8.1.2", "8.0.0-RC2", "8.0.0-SNAPSHOT", "7.0.0", "6.2.3", "3.3.1", "", "SNAPSHOT", "v9.0.0")) {
      assertFalse(version, GrailsCommandLine.isUsedBy(version));
    }
    assertFalse(GrailsCommandLine.isUsedBy(null));
  }

  public void testTheGradleImportTellsTheVersionOfAnApplication() throws Exception {
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("shop");
    assertTrue(Grails9Projects.isGrails9OrLater(GradleGrailsApplications.application(getProject(), root, "9.0.0-SNAPSHOT", List.of())));
    assertFalse(Grails9Projects.isGrails9OrLater(GradleGrailsApplications.application(getProject(), root, "8.1.2", List.of())));
    assertFalse(Grails9Projects.isGrails9OrLater(null));
  }

  public void testAnApplicationOutsideAGradleImportIsNoGrails9Application() {
    GrailsApplication application = (GrailsApplication)Proxy.newProxyInstance(
      GrailsApplication.class.getClassLoader(), new Class<?>[]{GrailsApplication.class},
      (proxy, method, args) -> {
        if (method.getName().equals("getGrailsVersion")) return new VersionImpl("9.0.0");
        throw new UnsupportedOperationException(method.getName());
      });
    assertFalse("only the Gradle import knows grailsVersion", Grails9Projects.isGrails9OrLater(application));
  }

  public void testTheRecordIsProjectYmlInDotGrails() {
    VirtualFile record = myFixture.addFileToProject("shop/.grails/project.yml", "format: 1\n").getVirtualFile();
    VirtualFile root = record.getParent().getParent();

    assertTrue(Grails9Projects.hasProjectRecord(root));
    assertEquals(record, Grails9Projects.findProjectRecord(root));
    assertEquals(root, Grails9Projects.projectRootOfRecord(record));
  }

  public void testOtherFilesAreNoRecord() throws Exception {
    VirtualFile withoutRecord = myFixture.addFileToProject("app/.grails/forge.lock.yml", "").getVirtualFile().getParent().getParent();
    assertFalse("a .grails directory without project.yml", Grails9Projects.hasProjectRecord(withoutRecord));

    VirtualFile elsewhere = myFixture.addFileToProject("other/config/project.yml", "").getVirtualFile();
    assertNull("a project.yml outside .grails", Grails9Projects.projectRootOfRecord(elsewhere));

    VirtualFile directory = myFixture.getTempDirFixture().findOrCreateDir("odd/.grails/project.yml");
    VirtualFile oddRoot = directory.getParent().getParent();
    assertFalse("a directory named project.yml", Grails9Projects.hasProjectRecord(oddRoot));
    assertNull(Grails9Projects.projectRootOfRecord(directory));

    VirtualFile fileNamedGrails = myFixture.addFileToProject("plain/.grails", "").getVirtualFile().getParent();
    assertFalse("a file named .grails", Grails9Projects.hasProjectRecord(fileNamedGrails));
  }

  public void testTheRecordDoesNotMakeAProjectOfGrails8OneOfGrails9() {
    // grails project migrate writes the record for an application that is still on Grails 8
    VirtualFile root = myFixture.addFileToProject("migrated/.grails/project.yml", "format: 1\n").getVirtualFile().getParent().getParent();
    Grails3Application grails8 = GradleGrailsApplications.application(getProject(), root, "8.1.2", List.of("/repo/grails-shell-cli-8.1.2.jar"));

    assertFalse(Grails9Projects.isGrails9OrLater(grails8));
    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails8), Grails3GradleCommandExecutor.class);
  }

  public void testAProjectOfGrails9WithoutARecordIsOneOfGrails9() throws Exception {
    // an application the Grails shell created, upgraded to Grails 9 before grails project migrate wrote its record
    VirtualFile root = myFixture.getTempDirFixture().findOrCreateDir("upgraded");
    Grails3Application grails9 = GradleGrailsApplications.application(getProject(), root, "9.0.0", List.of("/repo/grails-cli-9.0.0-all.jar"));

    assertFalse(Grails9Projects.hasProjectRecord(root));
    assertTrue(Grails9Projects.isGrails9OrLater(grails9));
    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails9), Grails9CommandExecutor.class);
  }
}

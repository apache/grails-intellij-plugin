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

package org.apache.grails.intellij.plugin.actions;

import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.PlatformTestUtil;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;

import java.util.ArrayList;
import java.util.List;

/**
 * The completion path of the "New Grails artefact" actions: which files a command is expected to
 * generate, and that the first of them is opened after a refresh scoped to those files, without a
 * directory-wide refresh and without running a Grails process.
 */
public class NewGrailsXXXActionTest extends GrailsTestCase {

  private GrailsApplication application() {
    return assertOneElement(GrailsApplicationManager.getInstance(getProject()).getApplications());
  }

  /** The package {@code _GrailsCreateArtifacts.createRootPackage()} puts a default-package artefact into. */
  private static String rootPackage(GrailsApplication application) {
    return StringUtil.toLowerCase(application.getRoot().getName().replace('-', '.'));
  }

  private VirtualFile addControllerFile(String relativePath) {
    return myFixture.addFileToProject(relativePath, "class BookController {}").getVirtualFile();
  }

  private void assertOpened(VirtualFile file) {
    FileEditorManager manager = FileEditorManager.getInstance(getProject());
    PlatformTestUtil.waitWithEventsDispatching("the generated file was not opened: " + file.getPath(),
                                               () -> manager.isFileOpen(file), 15);
  }

  public void testQualifiedArtefactIsExpectedUnderItsOwnPackage() {
    VirtualFile controller = addControllerFile("grails-app/controllers/com/example/BookController.groovy");
    List<VirtualFile> existing = new NewGrailsControllerAction().findExistingGeneratedFiles(application(), "com.example.Book");
    assertEquals(List.of(controller), existing);
  }

  public void testDefaultPackageArtefactIsExpectedUnderTheApplicationRootPackage() {
    GrailsApplication application = application();
    VirtualFile controller = addControllerFile("grails-app/controllers/" + rootPackage(application).replace('.', '/') + "/BookController.groovy");
    List<VirtualFile> existing = new NewGrailsControllerAction().findExistingGeneratedFiles(application, "Book");
    assertEquals("a dot-less name is generated into the application-root package", List.of(controller), existing);
  }

  public void testAfterCommandOpensTheGeneratedFile() {
    VirtualFile controller = addControllerFile("grails-app/controllers/com/example/BookController.groovy");
    new NewGrailsControllerAction().afterCommand(application(), "com.example.Book");
    assertOpened(controller);
  }

  public void testAfterCommandOpensADefaultPackageArtefact() {
    GrailsApplication application = application();
    VirtualFile controller = addControllerFile("grails-app/controllers/" + rootPackage(application).replace('.', '/') + "/BookController.groovy");
    new NewGrailsControllerAction().afterCommand(application, "Book");
    assertOpened(controller);
  }

  public void testInterceptorDeclaresItsGeneratedFiles() {
    List<String> expected = List.of("grails-app/controllers/com/example/AuthInterceptor.groovy",
                                    "src/test/groovy/com/example/AuthInterceptorSpec.groovy");
    List<String> names = new ArrayList<>();
    new NewGrailsInterceptorAction().fillGeneratedNamesList("com.example.Auth", names);
    assertEquals(expected, names);

    names.clear();
    new NewGrailsInterceptorAction().fillGeneratedNamesList("com.example.AuthInterceptor", names);
    assertEquals("the Interceptor suffix is not doubled", expected, names);
  }
}

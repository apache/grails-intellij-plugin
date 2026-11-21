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

  public void testAfterCommandOpensTheGeneratedFileWithFocus() {
    VirtualFile service = myFixture.addFileToProject("grails-app/services/shop/OrderService.groovy",
                                                     "package shop\nclass OrderService {}").getVirtualFile();
    new NewGrailsServiceAction().afterCommand(application(), "shop.Order");
    assertOpened(service);
    assertOrderedEquals(FileEditorManager.getInstance(getProject()).getSelectedFiles(), service);
  }

  public void testAfterCommandOpensOnlyTheFirstGeneratedFile() {
    VirtualFile service = myFixture.addFileToProject("grails-app/services/shop/OrderService.groovy",
                                                     "package shop\nclass OrderService {}").getVirtualFile();
    VirtualFile spec = myFixture.addFileToProject("test/unit/shop/OrderServiceSpec.groovy",
                                                  "package shop\nclass OrderServiceSpec {}").getVirtualFile();
    new NewGrailsServiceAction().afterCommand(application(), "shop.Order");
    assertOpened(service);
    assertFalse("only the artefact itself is opened, not its test", FileEditorManager.getInstance(getProject()).isFileOpen(spec));
  }

  public void testNothingIsOpenedWhenTheCommandGeneratedNoFile() {
    myFixture.addFileToProject("grails-app/services/shop/OtherService.groovy", "package shop\nclass OtherService {}");
    NewGrailsServiceAction action = new NewGrailsServiceAction();
    List<String> expected = new ArrayList<>();
    action.fillGeneratedNamesList("shop.Order", expected);

    // Run the lookup on this thread rather than through afterCommand's pooled thread, so the absence of an
    // editor can be asserted once the EDT queue has drained instead of after an arbitrary wait.
    action.openFirstGeneratedFile(application(), expected);
    PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue();

    assertEmpty(FileEditorManager.getInstance(getProject()).getOpenFiles());
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

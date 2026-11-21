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

package org.apache.grails.intellij.plugin;

import com.intellij.codeInsight.AttachSourcesProvider.AttachSourcesAction;
import com.intellij.ide.JavaUiBundle;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.application.WriteAction;
import com.intellij.openapi.application.impl.TestOnlyThreading;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.roots.OrderEnumerator;
import com.intellij.openapi.roots.OrderRootType;
import com.intellij.openapi.roots.libraries.Library;
import com.intellij.openapi.vfs.JarFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.platform.backend.workspace.WorkspaceModel;
import com.intellij.platform.workspace.jps.entities.LibraryEntity;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.LightProjectDescriptor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.testFramework.PsiTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import com.intellij.testFramework.fixtures.TempDirTestFixture;
import com.intellij.testFramework.fixtures.impl.TempDirTestFixtureImpl;
import com.intellij.workspaceModel.ide.legacyBridge.LibraryBridgesKt;
import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;

public class GrailsAttachSourcesProviderTest extends LightJavaCodeInsightFixtureTestCase {
  private final GrailsAttachSourcesProvider provider = new GrailsAttachSourcesProvider();
  private final List<Library> libraries = new ArrayList<>();

  @Override
  protected @NotNull LightProjectDescriptor getProjectDescriptor() {
    return new DefaultLightProjectDescriptor(IdeaTestUtil::getMockJdk11);
  }

  @Override
  protected @NotNull TempDirTestFixture getTempDirFixture() {
    // JarFileSystem requires real local files, not the light fixture's in-memory VFS.
    return new TempDirTestFixtureImpl();
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      for (Library library : libraries) {
        PsiTestUtil.removeLibrary(getModule(), library);
      }
      libraries.clear();
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  public void testAttachesExistingSourcesThroughEntityAction() throws IOException {
    VirtualFile classes = createJar("grails/grails-core-2.0.0.jar");
    VirtualFile sources = createJar("grails/src/grails-core-2.0.0-sources.jar");
    Library library = addLibrary(classes);
    PsiFile file = jarFile(classes);

    AttachSourcesAction action = assertOneElement(provider.getLibrariesActions(Set.of(entity(library)), file));
    assertEquals(GrailsBundle.message("attache.source.from.grails.action.title", "$GRAILS_HOME/src/"), action.getName());
    assertEquals(action.getName(), action.getBusyText());
    assertTrue(action.perform(Set.of(entity(library)), getProject()).isDone());
    assertSame(sources, assertOneElement(library.getFiles(OrderRootType.SOURCES)));
    assertEmpty(provider.getLibrariesActions(Set.of(entity(library)), file));

    assertTrue(action.perform(Set.of(entity(library)), getProject()).isDone());
    assertSize(1, library.getFiles(OrderRootType.SOURCES));
  }

  public void testDiscoversExistingSourcesInBackgroundReadAction() throws IOException {
    VirtualFile classes = createJar("grails/grails-core-2.0.0.jar");
    createJar("grails/src/grails-core-2.0.0-sources.jar");
    Library library = addLibrary(classes);
    LibraryEntity entity = entity(library);
    PsiFile file = jarFile(classes);
    var application = ApplicationManager.getApplication();
    assertFalse(application.isWriteAccessAllowed());

    // Release the EDT fixture's write-intent lock so pending background writes cannot block the read action.
    TestOnlyThreading.releaseTheAcquiredWriteIntentLockThenExecuteActionAndTakeWriteIntentLockBack(() -> {
      AttachSourcesAction action = PlatformTestUtil.callOnBgtSynchronously(() -> ReadAction.compute(() -> {
        assertFalse(application.isDispatchThread());
        assertTrue(application.isReadAccessAllowed());
        return assertOneElement(provider.getLibrariesActions(Set.of(entity), file));
      }), 10);
      assertNotNull(action);
      assertEquals(GrailsBundle.message("attache.source.from.grails.action.title", "$GRAILS_HOME/src/"), action.getName());
    });
    assertEmpty(library.getFiles(OrderRootType.SOURCES));
  }

  public void testOffersDownloadWhenSourcesAreMissing() throws IOException {
    VirtualFile classes = createJar("grails/grails-core-2.0.0.jar");
    myFixture.getTempDirFixture().findOrCreateDir("grails/src");
    Library library = addLibrary(classes);

    AttachSourcesAction action = assertOneElement(provider.getLibrariesActions(Set.of(entity(library)), jarFile(classes)));
    assertEquals(JavaUiBundle.message("attach.source.provider.download.sources.action.name"), action.getName());
    assertEquals(JavaUiBundle.message("attach.source.provider.download.sources.action.busy.text"), action.getBusyText());
    assertEmpty(library.getFiles(OrderRootType.SOURCES));
  }

  public void testIgnoresNonJarFiles() throws IOException {
    Library library = addLibrary(createJar("grails/grails-core-2.0.0.jar"));
    PsiFile file = myFixture.configureByText("plain.txt", "not in a jar");
    assertEmpty(provider.getLibrariesActions(Set.of(entity(library)), file));
  }

  public void testIgnoresEmptyLibraryCollection() throws IOException {
    VirtualFile classes = createJar("grails/grails-core-2.0.0.jar");
    assertEmpty(provider.getLibrariesActions(Set.of(), jarFile(classes)));
  }

  public void testIgnoresMultipleDistinctLibraries() throws IOException {
    VirtualFile classes = createJar("grails/grails-core-2.0.0.jar");
    createJar("grails/src/grails-core-2.0.0-sources.jar");
    Library first = addLibrary(classes);
    Library second = addLibrary(classes);
    assertEmpty(provider.getLibrariesActions(Set.of(entity(first), entity(second)), jarFile(classes)));
  }

  public void testAcceptsRepeatedEntityForSameLibrary() throws IOException {
    VirtualFile classes = createJar("grails/grails-core-2.0.0.jar");
    createJar("grails/src/grails-core-2.0.0-sources.jar");
    LibraryEntity entity = entity(addLibrary(classes));
    assertSize(1, provider.getLibrariesActions(List.of(entity, entity), jarFile(classes)));
  }

  public void testIgnoresLibraryWithoutCurrentBridge() throws IOException {
    VirtualFile classes = createJar("grails/grails-core-2.0.0.jar");
    createJar("grails/src/grails-core-2.0.0-sources.jar");
    Library library = addLibrary(classes);
    LibraryEntity entity = entity(library);
    PsiFile file = jarFile(classes);
    PsiTestUtil.removeLibrary(getModule(), library);
    libraries.remove(library);
    assertEmpty(provider.getLibrariesActions(Set.of(entity), file));
  }

  /**
   * A module-level library is in no library table, so it can only be found through the order entries
   * of the module that owns it.
   */
  public void testResolvesModuleLevelLibrary() throws IOException {
    VirtualFile classes = createJar("grails/grails-core-2.0.0.jar");
    VirtualFile sources = createJar("grails/src/grails-core-2.0.0-sources.jar");
    ModuleRootModificationUtil.updateModel(getModule(), model -> {
      Library.ModifiableModel library = model.getModuleLibraryTable().createLibrary("GrailsModuleLevel").getModifiableModel();
      library.addRoot(classes, OrderRootType.CLASSES);
      library.commit();
    });
    List<Library> found = new ArrayList<>();
    OrderEnumerator.orderEntries(getModule()).forEachLibrary(candidate -> {
      if ("GrailsModuleLevel".equals(candidate.getName())) found.add(candidate);
      return true;
    });
    Library library = assertOneElement(found);
    libraries.add(library);
    assertNull("the library must be module-level", library.getTable());

    AttachSourcesAction action = assertOneElement(provider.getLibrariesActions(Set.of(entity(library)), jarFile(classes)));
    assertTrue(action.perform(Set.of(entity(library)), getProject()).isDone());
    assertSame(sources, assertOneElement(library.getFiles(OrderRootType.SOURCES)));
  }

  /** The entity must be matched by id, not assumed to be whichever library happens to contain the file. */
  public void testPicksTheRequestedLibraryWhenTwoContainTheJar() throws IOException {
    VirtualFile classes = createJar("grails/grails-core-2.0.0.jar");
    VirtualFile sources = createJar("grails/src/grails-core-2.0.0-sources.jar");
    Library first = addLibrary(classes);
    Library second = addLibrary(classes);

    AttachSourcesAction action = assertOneElement(provider.getLibrariesActions(Set.of(entity(second)), jarFile(classes)));
    assertTrue(action.perform(Set.of(entity(second)), getProject()).isDone());

    assertSame(sources, assertOneElement(second.getFiles(OrderRootType.SOURCES)));
    assertEmpty(first.getFiles(OrderRootType.SOURCES));
  }

  public void testIgnoresNonGrailsLibrary() throws IOException {
    VirtualFile classes = createJar("other/other-2.0.0.jar");
    createJar("other/src/other-2.0.0-sources.jar");
    Library library = addLibrary(classes);
    assertEmpty(provider.getLibrariesActions(Set.of(entity(library)), jarFile(classes)));
  }

  public void testIgnoresGrailsBefore14() throws IOException {
    VirtualFile classes = createJar("grails/grails-core-1.3.7.jar");
    createJar("grails/src/grails-core-1.3.7-sources.jar");
    Library library = addLibrary(classes);
    assertEmpty(provider.getLibrariesActions(Set.of(entity(library)), jarFile(classes)));
  }

  private Library addLibrary(VirtualFile classes) {
    Library library = PsiTestUtil.addProjectLibrary(getModule(), "GrailsSources" + libraries.size(), List.of(classes), List.of());
    libraries.add(library);
    return library;
  }

  private LibraryEntity entity(Library library) {
    LibraryEntity entity = LibraryBridgesKt.findLibraryEntity(library, WorkspaceModel.getInstance(getProject()).getCurrentSnapshot());
    assertNotNull(entity);
    return entity;
  }

  private PsiFile jarFile(VirtualFile root) {
    VirtualFile entry = root.findChild("entry.txt");
    assertNotNull(entry);
    PsiFile file = PsiManager.getInstance(getProject()).findFile(entry);
    assertNotNull(file);
    return file;
  }

  private VirtualFile createJar(String path) throws IOException {
    ByteArrayOutputStream bytes = new ByteArrayOutputStream();
    try (JarOutputStream jar = new JarOutputStream(bytes)) {
      jar.putNextEntry(new JarEntry("entry.txt"));
      jar.write('x');
      jar.closeEntry();
    }
    VirtualFile file = myFixture.getTempDirFixture().createFile(path);
    WriteAction.run(() -> file.setBinaryContent(bytes.toByteArray()));
    VirtualFile root = JarFileSystem.getInstance().getJarRootForLocalFile(file);
    assertNotNull(root);
    return root;
  }
}

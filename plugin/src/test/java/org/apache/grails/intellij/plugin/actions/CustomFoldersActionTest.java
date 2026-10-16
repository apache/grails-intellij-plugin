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

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.Presentation;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.tree.LeafState;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersAppService;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersProjectService;
import org.apache.grails.intellij.plugin.projectView.impl.GrailsCustomFoldersNodeProvider;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsCustomFolder;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsCustomFolderNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsMissingCustomFolderNode;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;
import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


public class CustomFoldersActionTest extends GrailsTestCase {

  /**
   * The stored path must be the one the provider resolves: relative to the Grails application root.
   * The fixture's application root is the temp-dir root while {@code project.getBaseDir()} is
   * {@code /private/var/…/unitTest_*}, which is not its ancestor — so a project-relative base stored
   * a fully absolute path here, and the old test recomputed that same wrong formula on both sides.
   */
  public void testAddPathAppendsToProjectFolders() {
    PsiDirectory directory = createDirectory("custom");
    GrailsCustomFoldersProjectService service = service();
    service.setFolders(List.of("existing"));

    new AddCustomFolderAction().actionPerformed(
      event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, directory)));

    assertEquals("the path is stored relative to the application root, exactly",
                 List.of("existing", "custom"), service.getFolders());

    GrailsApplication application = GrailsApplicationManager.getInstance(getProject()).findApplication(
      directory.getVirtualFile());
    assertNotNull("the selected directory must belong to the fixture's application", application);

    GrailsCustomFolderNode node = existingFolderNode(nodeFor(application, "custom"));

    assertEquals("the stored path is the one the provider resolves", "custom", node.getConfiguredPath());
    assertFalse("the stored path resolves to an expandable node, not to a placeholder",
                node.getChildren().isEmpty());
  }

  /**
   * The reviewer's probe: a multi-module build keeps the application one level below the project.
   * The provider resolves a relative path against the application root, so the base is that root —
   * {@code docs} expands and the project-relative spelling {@code app/docs} is a placeholder.
   */
  public void testPathIsRelativeToTheApplicationRootNotTheProjectDir() {
    myFixture.addFileToProject("app/docs/readme.md", "# readme");
    VirtualFile applicationRoot = Objects.requireNonNull(myFixture.getTempDirFixture().getFile("app"));
    GrailsApplication application = new SubdirectoryApplication(getProject(), applicationRoot);
    GrailsCustomFoldersProjectService service = service();

    service.setFolders(List.of("docs"));
    GrailsCustomFolderNode node = existingFolderNode(nodeFor(application, "docs"));
    assertEquals("docs is relative to the application root", "docs", node.getConfiguredPath());
    assertFalse("docs resolves against the application root, so it expands",
                node.getChildren().isEmpty());

    service.setFolders(List.of("app/docs"));
    GrailsMissingCustomFolderNode placeholder = placeholderNode(nodeFor(application, "app/docs"));
    assertEquals("the project-relative spelling resolves to nothing, so the node is a placeholder",
                 LeafState.ALWAYS, placeholder.getLeafState());
    assertTrue("a placeholder has nothing to expand", placeholder.getChildren().isEmpty());
  }

  // The application root is already the top of the Grails view; adding it as a custom folder would
  // duplicate the whole application under the application node, so the action stays hidden for it.
  public void testHiddenForApplicationRoot() {
    PsiDirectory root = applicationRoot();
    assertNotNull(root);

    AddCustomFolderAction action = new AddCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, root));

    action.update(event);

    assertFalse("the application root must not offer itself as a custom folder",
                event.getPresentation().isVisible());
  }

  // A directory that belongs to no Grails application offers nothing to add: there is no application
  // root to resolve a configured path against.
  public void testHiddenForDirectoryOutsideAnyApplication() {
    PsiDirectory baseDir = PsiManager.getInstance(getProject()).findDirectory(getProject().getBaseDir());
    assertNotNull(baseDir);

    AddCustomFolderAction action = new AddCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, baseDir));

    action.update(event);

    assertFalse("a directory outside every application is not a custom folder candidate",
                event.getPresentation().isVisible());
  }

  /**
   * Gradle marks {@code build/} and {@code .gradle/} excluded output directories on import, and an
   * excluded file has no content root — which is where {@code findApplication} used to start its
   * upward walk. Both are the working folders the feature exists for, so both must still offer
   * themselves, and both must store the path the provider resolves.
   */
  public void testExcludedBuildAndGradleDirectoriesAreOffered() {
    GrailsCustomFoldersProjectService service = service();
    service.setFolders(List.of());

    for (String name : List.of("build", ".gradle")) {
      PsiDirectory directory = createDirectory(name);
      exclude(name);

      GrailsApplication application = GrailsApplicationManager.getInstance(getProject())
        .findApplication(directory.getVirtualFile());
      assertNotNull("an excluded directory under the application root still belongs to it: " + name,
                    application);

      AddCustomFolderAction action = new AddCustomFolderAction();
      AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, directory));

      action.update(event);
      assertTrue("the excluded " + name + " directory must offer itself as a custom folder",
                 event.getPresentation().isVisible());
      assertTrue(event.getPresentation().isEnabled());

      action.actionPerformed(event(data(CommonDataKeys.PROJECT, getProject(),
                                       CommonDataKeys.PSI_ELEMENT, directory)));
    }

    assertEquals("both excluded directories are stored relative to the application root",
                 List.of("build", ".gradle"), service.getFolders());
  }

  /**
   * The companion to the excluded case: resolving an excluded directory must not start accepting
   * directories from outside the project. A path with no content root and no project-directory
   * ancestor gets no walk at all, so it still resolves to nothing.
   */
  public void testExcludedResolutionStaysInsideTheProject() throws Exception {
    File outsideDir = new File(FileUtil.getTempDirectory(), "grails-custom-folders-outside");
    FileUtil.createDirectory(outsideDir);
    VirtualFile outside = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(outsideDir);
    assertNotNull("the test needs a real directory outside the project", outside);
    try {
      assertNull("a directory outside the project resolves to no application, excluded or not",
                 GrailsApplicationManager.getInstance(getProject()).findApplication(outside));
    }
    finally {
      FileUtil.delete(outsideDir);
    }
  }

  /** Marks {@code <temp>/{name}} excluded, the way Gradle marks build/ and .gradle/ on import. */
  private void exclude(@NotNull String name) {
    VirtualFile dir = Objects.requireNonNull(myFixture.findFileInTempDir(name + "/placeholder.txt").getParent());
    com.intellij.openapi.roots.ModuleRootModificationUtil.updateModel(getModule(), model ->
      model.getContentEntries()[0].addExcludeFolder(dir));
  }

  // Guard against invocation that bypasses update() (e.g. a user-mapped shortcut): the application
  // root is never stored as a custom folder.
  public void testAddRootIsNoOp() {
    PsiDirectory root = applicationRoot();
    assertNotNull(root);

    new AddCustomFolderAction().actionPerformed(
      event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, root)));

    assertTrue(service().getFolders().isEmpty());
  }

  // update() must be safe before a context exists at all: with no project in the data context there
  // is nothing the action could be offered for.
  public void testAddUpdateInvisibleWithoutProject() {
    AddCustomFolderAction action = new AddCustomFolderAction();
    AnActionEvent event = event(new HashMap<>());

    action.update(event);

    assertFalse(event.getPresentation().isVisible());
    assertFalse(event.getPresentation().isEnabled());
  }

  public void testAddUpdateInvisibleWithoutSelectedDirectory() {
    AddCustomFolderAction action = new AddCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject()));

    action.update(event);

    assertFalse("a project with no directory selected offers nothing to add",
                event.getPresentation().isVisible());
  }

  // Under the cap and inside an application, the action is offered: this is the branch the
  // disappearing-cap guard above has to leave open.
  public void testAddUpdateEnabledForDirectoryInsideApplication() {
    PsiDirectory directory = createDirectory("custom");
    AddCustomFolderAction action = new AddCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, directory));

    action.update(event);

    assertTrue("a directory inside the application, under the cap, can be added",
               event.getPresentation().isVisible());
    assertTrue(event.getPresentation().isEnabled());
  }

  /**
   * The data context does not always carry a PSI element: some panes snapshot the selected file and
   * not the element, so the action has to resolve the directory itself. {@code update()} runs on BGT,
   * so it is driven from a background thread here — the EDT holds no read action of its own, so the
   * action's one is wrapped in {@code ReadAction.computeBlocking}.
   */
  public void testAddUpdateReadsADirectoryFromTheVirtualFileAlone() {
    PsiDirectory directory = createDirectory("custom");
    AddCustomFolderAction action = new AddCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(),
                                     CommonDataKeys.VIRTUAL_FILE, directory.getVirtualFile()));

    Presentation presentation = updateOffEdt(action, event);

    assertTrue("a directory carried only as a VirtualFile is still a candidate",
               presentation.isVisible());
    assertTrue(presentation.isEnabled());
  }

  /**
   * Runs {@code update()} the way the action system does, on a background thread rather than the
   * EDT, so the action is exercised under the threading it is declared with.
   */
  private static @NotNull Presentation updateOffEdt(@NotNull AnAction action,
                                                    @NotNull AnActionEvent event) {
    try {
      return PlatformTestUtil.waitForFuture(
        ApplicationManager.getApplication().executeOnPooledThread(() -> {
          action.update(event);
          return event.getPresentation();
        }), 10_000);
    }
    catch (Exception e) {
      throw new IllegalStateException("update() could not be run off the EDT", e);
    }
  }

  /**
   * The reported no-op: right-clicking a folder the Grails view already renders still offered
   * *Add as Custom Folder*, and choosing it stored nothing. The node is right there under the
   * application, so the action is now hidden rather than offered dead.
   */
  public void testAddUpdateInvisibleForAlreadyConfiguredFolder() {
    PsiDirectory directory = createDirectory("docs");
    service().setFolders(List.of("docs"));

    AddCustomFolderAction action = new AddCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, directory));

    action.update(event);

    assertFalse("a folder the Grails view already renders must not offer Add again",
                event.getPresentation().isVisible());
  }

  /**
   * The comparison is normalized, so a configured {@code docs/} still hides the action for a selected
   * {@code docs}: the provider reads them as the same folder, so the action would be a no-op even
   * though the strings differ.
   */
  public void testAddUpdateInvisibleWhenConfiguredUnderAnotherSpelling() {
    PsiDirectory directory = createDirectory("docs");
    service().setFolders(List.of("docs/"));

    AddCustomFolderAction action = new AddCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, directory));

    action.update(event);

    assertFalse("a spelling variant of an already-rendered folder must not offer Add",
                event.getPresentation().isVisible());
  }

  /**
   * A global default renders in this project's view too, so it counts as already configured even
   * though this project's own list is empty.
   */
  public void testAddUpdateInvisibleForGlobalDefaultFolder() {
    PsiDirectory directory = createDirectory("docs");
    GrailsCustomFoldersAppService appService = GrailsCustomFoldersAppService.getStaticInstance();
    List<String> original = new ArrayList<>(appService.getFolders());
    try {
      appService.setFolders(List.of("docs"));
      service().setFolders(List.of());

      AddCustomFolderAction action = new AddCustomFolderAction();
      AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, directory));

      action.update(event);

      assertFalse("a folder inherited from the global defaults must not offer Add",
                  event.getPresentation().isVisible());
    }
    finally {
      appService.setFolders(original);
    }
  }

  /**
   * Hiding must be specific to the configured folder and not a blanket suppression: with two
   * directories present, the configured one is hidden and the unconfigured sibling is still offered
   * and enabled.
   */
  public void testAddUpdateStaysVisibleForAnUnconfiguredSibling() {
    PsiDirectory configured = createDirectory("docs");
    PsiDirectory other = createDirectory("other");
    service().setFolders(List.of("docs"));

    AddCustomFolderAction action = new AddCustomFolderAction();

    AnActionEvent hiddenEvent = event(data(CommonDataKeys.PROJECT, getProject(),
                                           CommonDataKeys.PSI_ELEMENT, configured));
    action.update(hiddenEvent);
    assertFalse("the configured folder is hidden", hiddenEvent.getPresentation().isVisible());

    AnActionEvent siblingEvent = event(data(CommonDataKeys.PROJECT, getProject(),
                                            CommonDataKeys.PSI_ELEMENT, other));
    action.update(siblingEvent);
    assertTrue("an unconfigured sibling is still offered", siblingEvent.getPresentation().isVisible());
    assertTrue(siblingEvent.getPresentation().isEnabled());
  }

  // The cap is a setting, not a constant, so this reads the configured value rather than assuming a
  // number: at the cap the action stays visible but disabled, one folder below it is enabled again.
  public void testDisabledAtTheCap() {
    PsiDirectory directory = createDirectory("custom");
    GrailsCustomFoldersProjectService service = service();
    int cap = GrailsCustomFoldersAppService.getStaticInstance().getMaxFolders();

    service.setFolders(folderNames(cap));

    AddCustomFolderAction action = new AddCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, directory));

    action.update(event);
    assertFalse("at the cap there is nothing left the action could add, so it is offered but disabled",
                event.getPresentation().isEnabled());
    assertTrue(event.getPresentation().isVisible());

    service.setFolders(folderNames(cap - 1));
    action.update(event);
    assertTrue("under the cap the action is offered and enabled",
               event.getPresentation().isEnabled());
    assertTrue(event.getPresentation().isVisible());
  }

  /**
   * The cap counts the folders the Grails view will show, which is this project's folders plus the
   * global defaults: one project folder and two global ones already reach a cap of three.
   */
  public void testDisabledWhenGlobalDefaultsFillTheCap() {
    PsiDirectory directory = createDirectory("custom");
    GrailsCustomFoldersProjectService service = service();
    GrailsCustomFoldersAppService appService = GrailsCustomFoldersAppService.getStaticInstance();
    List<String> originalGlobal = new ArrayList<>(appService.getFolders());
    int originalMax = appService.getMaxFolders();
    try {
      appService.setFolders(List.of("global1", "global2"));
      appService.setMaxFolders(3);
      service.setFolders(List.of("a"));

      AddCustomFolderAction action = new AddCustomFolderAction();
      AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, directory));

      action.update(event);

      assertFalse("the cap is reached by the global defaults this project did not own",
                  event.getPresentation().isEnabled());
      assertTrue(event.getPresentation().isVisible());
    }
    finally {
      appService.setFolders(originalGlobal);
      appService.setMaxFolders(originalMax);
    }
  }

  /**
   * update() hides the action for a folder the view already renders, and actionPerformed must agree
   * even when it is reached without update() — through a user-mapped shortcut, say. Storing it
   * anyway would copy a global default into this project's own list and flip the settings page's
   * source indicator, which is a change the user never asked for.
   */
  public void testAddActionDoesNotPromoteAGlobalDefaultIntoTheProjectList() {
    PsiDirectory directory = createDirectory("docs");
    GrailsCustomFoldersAppService appService = GrailsCustomFoldersAppService.getStaticInstance();
    int originalMax = appService.getMaxFolders();
    List<String> originalGlobal = new ArrayList<>(appService.getFolders());
    try {
      appService.setFolders(List.of("docs"));
      appService.setMaxFolders(6);
      service().setFolders(List.of());

      new AddCustomFolderAction().actionPerformed(
        event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, directory)));

      assertEquals("a folder inherited from the global defaults stays where it was",
                   List.of(), service().getFolders());
    }
    finally {
      appService.setFolders(originalGlobal);
      appService.setMaxFolders(originalMax);
    }
  }

  private static @NotNull List<String> folderNames(int count) {
    List<String> folders = new ArrayList<>();
    for (int i = 0; i < count; i++) {
      folders.add("folder" + i);
    }
    return folders;
  }

  /**
   * The action owns the removal: this drives it the way the pane does, by putting the selected node
   * into the {@code SELECTED_ITEMS} snapshot of the data context, and asserts on the service. The
   * placeholder node is used because a configured path that no longer resolves on disk is still
   * removable from the Grails view, and it needs no PsiDirectory to build.
   */
  public void testRemoveLogicRemovesConfiguredPathFromService() {
    GrailsCustomFoldersProjectService service = service();
    service.setFolders(new ArrayList<>(List.of("src/main/groovy", "grails-app/domain")));

    RemoveCustomFolderAction action = new RemoveCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(),
                                     PlatformCoreDataKeys.SELECTED_ITEMS,
                                     new Object[] {
                                       new GrailsMissingCustomFolderNode(getProject(), ViewSettings.DEFAULT, "src/main/groovy")
                                     }));

    action.update(event);
    assertTrue("the selected folder is this project's own, so the action is offered",
               event.getPresentation().isVisible());

    action.actionPerformed(event);

    assertEquals(List.of("grails-app/domain"), service.getFolders());
  }

  // The snapshot can hold several entries, and the action must take the last custom folder in it:
  // that is the one the user acted on.
  public void testRemoveTakesTheLastSelectedCustomFolder() {
    GrailsCustomFoldersProjectService service = service();
    service.setFolders(List.of("first", "last"));

    RemoveCustomFolderAction action = new RemoveCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(),
                                     PlatformCoreDataKeys.SELECTED_ITEMS,
                                     new Object[] {
                                       new GrailsMissingCustomFolderNode(getProject(), ViewSettings.DEFAULT, "first"),
                                       new GrailsMissingCustomFolderNode(getProject(), ViewSettings.DEFAULT, "last"),
                                     }));

    action.actionPerformed(event);

    assertEquals("the last selected custom folder is the one removed", List.of("first"), service.getFolders());
  }

  public void testRemoveActionIsNoOpWithoutSelectedCustomFolder() {
    GrailsCustomFoldersProjectService service = service();
    service.setFolders(new ArrayList<>(List.of("src/main/groovy", "grails-app/domain")));

    RemoveCustomFolderAction action = new RemoveCustomFolderAction();
    action.actionPerformed(event(data(CommonDataKeys.PROJECT, getProject())));

    assertEquals(List.of("src/main/groovy", "grails-app/domain"), service.getFolders());
  }

  // Without a custom folder in the SELECTED_ITEMS snapshot there is nothing to remove, whatever else
  // the data context holds.
  public void testRemoveUpdateInvisibleWithoutSelectedCustomFolder() {
    RemoveCustomFolderAction action = new RemoveCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject()));

    action.update(event);

    assertFalse(event.getPresentation().isEnabled());
    assertFalse(event.getPresentation().isVisible());
  }

  // The placeholder kind satisfies the shared interface too: a configured path that resolves to
  // nothing stays removable, so it must still reach the action.
  public void testRemoveAcceptsTheMissingFolderPlaceholder() {
    assertEquals("the placeholder exposes the configured path",
                 "src/main/groovy",
                 new GrailsMissingCustomFolderNode(getProject(), ViewSettings.DEFAULT, "src/main/groovy").getConfiguredPath());
  }

  public void testRemoveVisibleForProjectFolder() {
    service().setFolders(List.of("docs"));

    assertTrue("a folder this project owns can be removed",
               RemoveCustomFolderAction.isProjectFolder(getProject(), "docs"));
    assertTrue("a project folder written with a leading ./ is still recognized",
               RemoveCustomFolderAction.isProjectFolder(getProject(), "./docs"));
  }

  // A node can come from the global defaults, which this project does not own; removing one would
  // silently do nothing, so the action stays hidden for it.
  public void testRemoveHiddenForGlobalFolder() {
    service().setFolders(List.of("docs"));

    RemoveCustomFolderAction action = new RemoveCustomFolderAction();
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(),
                                     PlatformCoreDataKeys.SELECTED_ITEMS,
                                     new Object[] {
                                       new GrailsMissingCustomFolderNode(getProject(), ViewSettings.DEFAULT, "global/folder")
                                     }));

    action.update(event);

    assertFalse("a global-only folder is not this project's to remove, so the action stays hidden",
                event.getPresentation().isVisible());
    assertFalse("nor is a folder this project never configured one",
                RemoveCustomFolderAction.isProjectFolder(getProject(), "other"));
  }

  public void testRemoveDoesNotThrowWithoutProject() {
    new RemoveCustomFolderAction().actionPerformed(event(new HashMap<>()));
  }

  public void testConfigureUpdateInvisibleWithoutProject() {
    ConfigureCustomFoldersAction action = new ConfigureCustomFoldersAction();
    AnActionEvent event = event(new HashMap<>());

    action.update(event);

    assertFalse(event.getPresentation().isEnabled());
    assertFalse(event.getPresentation().isVisible());
  }

  // FR27: the settings dialog edits the Grails pane's folders, so it is offered in that pane alone.
  // A light fixture's current pane is either absent or the platform's Project pane — neither is the
  // Grails pane, which is what hides the action here even though the data context would qualify.
  public void testConfigureUpdateInvisibleWhenCurrentPaneIsNotTheGrailsPane() {
    ConfigureCustomFoldersAction action = new ConfigureCustomFoldersAction();
    PsiDirectory directory = createDirectory("custom");
    AnActionEvent event = event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, directory));

    assertFalse("the fixture's current pane is not the Grails pane",
                ConfigureCustomFoldersAction.isGrailsPane(getProject()));
    assertFalse("a project with no current pane at all is not the Grails pane either",
                ConfigureCustomFoldersAction.isGrailsPane(
                  ProjectView.getInstance(getProject()).getCurrentProjectViewPane()));

    action.update(event);

    assertFalse("the action is hidden outside the Grails pane, for a directory it would otherwise accept",
                event.getPresentation().isVisible());
  }

  // With no Grails pane to key on in the light fixture, the two selection shapes the action accepts
  // are asserted on the extractors it uses. A custom folder in the snapshot is one of them.
  public void testConfigureAcceptsASelectedCustomFolder() {
    assertTrue("a custom folder in the SELECTED_ITEMS snapshot is a selection the action accepts",
               ConfigureCustomFoldersAction.isCustomFolderSelected(
                 event(data(CommonDataKeys.PROJECT, getProject(),
                            PlatformCoreDataKeys.SELECTED_ITEMS,
                            new Object[] {new GrailsMissingCustomFolderNode(getProject(), ViewSettings.DEFAULT, "src/main/groovy")}))));

    assertFalse("a snapshot with no custom folder in it is not one",
                ConfigureCustomFoldersAction.isCustomFolderSelected(
                  event(data(CommonDataKeys.PROJECT, getProject(),
                             PlatformCoreDataKeys.SELECTED_ITEMS, new Object[0]))));
  }

  // The snapshot holds whatever the tree had selected, so a custom folder can be preceded by nodes
  // the action has nothing to do with; it must still be found.
  public void testConfigureAcceptsACustomFolderAmongOtherSelectedNodes() {
    assertTrue(ConfigureCustomFoldersAction.isCustomFolderSelected(
      event(data(CommonDataKeys.PROJECT, getProject(),
                 PlatformCoreDataKeys.SELECTED_ITEMS,
                 new Object[] {
                   new GrailsMissingCustomFolderNode(getProject(), ViewSettings.DEFAULT, "other"),
                   new GrailsMissingCustomFolderNode(getProject(), ViewSettings.DEFAULT, "docs"),
                 }))));
  }

  // A selected directory is the other shape, reached through the data context: as a PSI element or
  // as a directory VirtualFile, never through the pane's Swing tree.
  public void testConfigureAcceptsASelectedDirectory() {
    PsiDirectory directory = createDirectory("custom");

    PsiDirectory fromPsiElement = ConfigureCustomFoldersAction.selectedDirectory(
      event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.PSI_ELEMENT, directory)));
    assertEquals("a selected directory is taken from PSI_ELEMENT", directory, fromPsiElement);

    PsiDirectory fromFile = ConfigureCustomFoldersAction.selectedDirectory(
      event(data(CommonDataKeys.PROJECT, getProject(), CommonDataKeys.VIRTUAL_FILE, directory.getVirtualFile())));
    assertEquals("a selected directory is also reachable from VIRTUAL_FILE", directory, fromFile);

    assertNull("with no directory in the data context there is nothing to accept",
               ConfigureCustomFoldersAction.selectedDirectory(
                 event(data(CommonDataKeys.PROJECT, getProject()))));
  }

  /**
   * FR55: the four shapes the settings action must be offered for, driven through its visibility
   * rule because a light fixture has no Grails pane — the pane precondition is supplied here as an
   * argument, and the end-to-end path (the real pane being current, which is what {@code update()}
   * looks up) is a {@code runIde} observation, not a test.
   */
  public void testConfigureOfferedForEverySelectionInsideTheGrailsPane() {
    assertTrue("a custom folder selected in the Grails pane is offered the settings",
               ConfigureCustomFoldersAction.offersSettings(true, true, false));
    assertTrue("a directory selected in the Grails pane is offered the settings",
               ConfigureCustomFoldersAction.offersSettings(true, false, true));
  }

  /**
   * The same rule with nothing selected: the settings are reachable from the pane's own context
   * menu, so a selection that is neither a custom folder nor a directory offers nothing.
   */
  public void testConfigureHiddenInTheGrailsPaneWithoutASelectionToConfigure() {
    assertFalse("with no custom folder and no directory selected there is nothing to configure",
                ConfigureCustomFoldersAction.offersSettings(true, false, false));
  }

  /** FR27: outside the Grails pane the settings are not on offer, whatever the selection is. */
  public void testConfigureHiddenOutsideTheGrailsPane() {
    assertFalse("a custom folder selected outside the Grails pane must not offer the settings",
                ConfigureCustomFoldersAction.offersSettings(false, true, false));
    assertFalse("a directory selected outside the Grails pane must not offer the settings",
                ConfigureCustomFoldersAction.offersSettings(false, false, true));
    assertFalse("and neither is an empty selection",
                ConfigureCustomFoldersAction.offersSettings(false, false, false));
  }

  public void testConfigureDoesNotThrowWithoutProject() {
    new ConfigureCustomFoldersAction().actionPerformed(event(new HashMap<>()));
  }

  public void testActionsAreRegistered() {
    ActionManager actionManager = ActionManager.getInstance();
    assertTrue(actionManager.getAction("Grails.AddCustomFolder") instanceof AddCustomFolderAction);
    assertTrue(actionManager.getAction("Grails.RemoveCustomFolder") instanceof RemoveCustomFolderAction);
    assertTrue(actionManager.getAction("Grails.ConfigureCustomFolders") instanceof ConfigureCustomFoldersAction);
  }

  /** The node the provider builds for one configured path, so both node kinds can be pinned. */
  private @NotNull GrailsCustomFolder nodeFor(@NotNull GrailsApplication application, @NotNull String path) {
    Collection<GrailsCustomFolder> nodes = customFolderNodes(
      new GrailsCustomFoldersNodeProvider().createNodes(application, ViewSettings.DEFAULT));
    for (GrailsCustomFolder node : nodes) {
      if (path.equals(node.getConfiguredPath())) return node;
    }
    throw new AssertionError("no node for " + path + " among " + nodes);
  }

  private static @NotNull List<GrailsCustomFolder> customFolderNodes(
    @NotNull Collection<AbstractTreeNode<?>> nodes) {
    return nodes.stream().filter(GrailsCustomFolder.class::isInstance).map(GrailsCustomFolder.class::cast).toList();
  }

  private static @NotNull GrailsCustomFolderNode existingFolderNode(@NotNull GrailsCustomFolder folder) {
    if (folder instanceof GrailsCustomFolderNode existing) return existing;
    throw new AssertionError("expected an existing custom folder node, got " + folder);
  }

  private static @NotNull GrailsMissingCustomFolderNode placeholderNode(@NotNull GrailsCustomFolder folder) {
    if (folder instanceof GrailsMissingCustomFolderNode placeholder) return placeholder;
    throw new AssertionError("expected a placeholder custom folder node, got " + folder);
  }

  private @NotNull PsiDirectory applicationRoot() {
    GrailsApplication application = GrailsApplicationManager.getInstance(getProject()).getApplications().iterator().next();
    assertNotNull(application);
    return PsiManager.getInstance(getProject()).findDirectory(application.getRoot());
  }

  private GrailsCustomFoldersProjectService service() {
    return getProject().getService(GrailsCustomFoldersProjectService.class);
  }

  private PsiDirectory createDirectory(String path) {
    myFixture.addFileToProject(path + "/placeholder.txt", "content");
    VirtualFile file = myFixture.findFileInTempDir(path + "/placeholder.txt");
    assertNotNull(file);
    PsiDirectory directory = PsiManager.getInstance(getProject()).findDirectory(Objects.requireNonNull(file.getParent()));
    assertNotNull(directory);
    return directory;
  }

  private static Map<DataKey<?>, Object> data(DataKey<?> key, Object value) {
    Map<DataKey<?>, Object> data = new HashMap<>();
    data.put(key, value);
    return data;
  }

  private static Map<DataKey<?>, Object> data(DataKey<?> key1, Object value1, DataKey<?> key2, Object value2) {
    Map<DataKey<?>, Object> data = new HashMap<>();
    data.put(key1, value1);
    data.put(key2, value2);
    return data;
  }

  private static AnActionEvent event(Map<DataKey<?>, Object> data) {
    DataContext context = new DataContext() {
      @Override
      public @Nullable Object getData(@NotNull String dataId) {
        return null;
      }

      @Override
      @SuppressWarnings("unchecked")
      public <T> T getData(@NotNull DataKey<T> key) {
        return (T)data.get(key);
      }
    };
    return new AnActionEvent(null, context, ActionPlaces.UNKNOWN, new Presentation(), ActionManager.getInstance(), 0);
  }

  /**
   * Stands in for the multi-module layout the reviewer reproduced: an application whose root is a
   * subdirectory of the project, so {@code findApplication} on a directory inside it returns this
   * root and a path stored against it is the one the provider resolves.
   */
  private static final class SubdirectoryApplication extends UserDataHolderBase implements GrailsApplication {

    private final @NotNull Project myProject;
    private final @NotNull VirtualFile myRoot;

    private SubdirectoryApplication(@NotNull Project project, @NotNull VirtualFile root) {
      myProject = project;
      myRoot = root;
    }

    @Override
    public @NotNull String getName() {
      return "subApp";
    }

    @Override
    public @NotNull Icon getIcon() {
      return AllIcons.Nodes.Package;
    }

    @Override
    public @NotNull Project getProject() {
      return myProject;
    }

    @Override
    public @NotNull VirtualFile getRoot() {
      return myRoot;
    }

    @Override
    public @NotNull VirtualFile getAppRoot() {
      VirtualFile appRoot = myRoot.findChild("grails-app");
      return appRoot == null ? myRoot : appRoot;
    }

    @Override
    public @NotNull Version getGrailsVersion() {
      return Version.GRAILS_6_0;
    }

    @Override
    public boolean isValid() {
      return true;
    }

    @Override
    public void invalidate() {
    }

    @Override
    public @NotNull GlobalSearchScope getScope(boolean includeDependencies, boolean testsOnly) {
      return GlobalSearchScope.allScope(myProject);
    }
  }
}

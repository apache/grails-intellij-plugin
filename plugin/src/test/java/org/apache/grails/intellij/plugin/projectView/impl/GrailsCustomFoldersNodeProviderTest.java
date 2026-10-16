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

package org.apache.grails.intellij.plugin.projectView.impl;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.Computable;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.tree.LeafState;
import com.intellij.ui.tree.TreeVisitor;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersAppService;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersProjectService;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsCustomFolder;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsCustomFolderNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsMissingCustomFolderNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsPsiDirectoryNode;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;

import javax.swing.tree.TreePath;

public class GrailsCustomFoldersNodeProviderTest extends GrailsNodeProviderTestSupport {

  public void testNodesCreatedForEachConfiguredFolder() {
    myFixture.addFileToProject("docs/readme.md", "# readme");
    myFixture.addFileToProject(".github/workflows/ci.yml", "jobs: {}");
    myFixture.addFileToProject("build/output.txt", "stray");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("docs", ".github", "build"));

    List<GrailsCustomFolder> nodes = customFolderNodes(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

    assertEquals("one node per configured folder", 3, nodes.size());
    for (GrailsCustomFolder node : nodes) {
      assertEquals("every custom folder node carries the custom-folder weight",
                   NodeWeights.CUSTOM_FOLDER, node.getNodeWeight());
    }
    assertEquals("each node is labelled with its folder name",
                 List.of(".github", "build", "docs"),
                 nodes.stream().map(GrailsCustomFoldersNodeProviderTest::label).toList());
  }

  public void testNodesCreatedForGlobalAndProjectFolders() {
    myFixture.addFileToProject("docs/readme.md", "# readme");
    myFixture.addFileToProject("src/placeholder.txt", "");

    withGlobalFolders(List.of("docs"), () -> {
      GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
      service.setFolders(List.of("src"));

      List<GrailsCustomFolder> nodes = customFolderNodes(
        new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

      assertEquals("global defaults and project folders each produce a node",
                   List.of("docs", "src"),
                   nodes.stream().map(GrailsCustomFoldersNodeProviderTest::label).toList());
    });
  }

  /**
   * The rendered nodes are capped at the configured maximum, with the project's own folders taking
   * precedence over the global defaults. The cap is the setting, so the helper sets it to three.
   */
  public void testNodesCappedAtTheConfiguredMaximumWithProjectFoldersFirst() {
    myFixture.addFileToProject("global1/placeholder.txt", "");
    myFixture.addFileToProject("global2/placeholder.txt", "");
    myFixture.addFileToProject("custom1/placeholder.txt", "");
    myFixture.addFileToProject("custom2/placeholder.txt", "");

    withGlobalFolders(List.of("global1", "global2"), 3, () -> {
      GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
      service.setFolders(List.of("custom1", "custom2"));

      List<GrailsCustomFolder> nodes = customFolderNodes(
        new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

      assertEquals("two custom and one global folder are rendered",
                   List.of("custom1", "custom2", "global1"),
                   nodes.stream().map(GrailsCustomFoldersNodeProviderTest::label).toList());
    });
  }

  /**
   * The same five folders under a cap of six: raising the setting renders all of them, which is what
   * makes the cap a setting rather than a constant.
   */
  public void testNodesRenderEveryFolderWhenTheCapIsRaised() {
    myFixture.addFileToProject("global1/placeholder.txt", "");
    myFixture.addFileToProject("global2/placeholder.txt", "");
    myFixture.addFileToProject("custom1/placeholder.txt", "");
    myFixture.addFileToProject("custom2/placeholder.txt", "");
    myFixture.addFileToProject("custom3/placeholder.txt", "");

    withGlobalFolders(List.of("global1", "global2"), 6, () -> {
      GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
      service.setFolders(List.of("custom1", "custom2", "custom3"));

      List<GrailsCustomFolder> nodes = customFolderNodes(
        new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

      assertEquals("a cap of six renders all five configured folders",
                   List.of("custom1", "custom2", "custom3", "global1", "global2"),
                   nodes.stream().map(GrailsCustomFoldersNodeProviderTest::label).toList());
    });
  }

  /** Three project folders fill the budget, so no global folder gets a node. */
  public void testThreeProjectFoldersExcludeGlobalFolders() {
    myFixture.addFileToProject("global1/placeholder.txt", "");
    myFixture.addFileToProject("custom1/placeholder.txt", "");
    myFixture.addFileToProject("custom2/placeholder.txt", "");
    myFixture.addFileToProject("custom3/placeholder.txt", "");

    withGlobalFolders(List.of("global1"), 3, () -> {
      GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
      service.setFolders(List.of("custom1", "custom2", "custom3"));

      List<GrailsCustomFolder> nodes = customFolderNodes(
        new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

      assertEquals("only the three custom folders are rendered",
                   List.of("custom1", "custom2", "custom3"),
                   nodes.stream().map(GrailsCustomFoldersNodeProviderTest::label).toList());
    });
  }

  private void withGlobalFolders(@NotNull List<String> folders, @NotNull Runnable body) {
    withGlobalFolders(folders, GrailsCustomFoldersAppService.getStaticInstance().getMaxFolders(), body);
  }

  /** {@code body} runs with the global defaults and the cap both set; both are restored after it. */
  private void withGlobalFolders(@NotNull List<String> folders, int maxFolders, @NotNull Runnable body) {
    GrailsCustomFoldersAppService appService = GrailsCustomFoldersAppService.getStaticInstance();
    List<String> original = new ArrayList<>(appService.getFolders());
    int originalMax = appService.getMaxFolders();
    try {
      appService.setFolders(folders);
      appService.setMaxFolders(maxFolders);
      body.run();
    }
    finally {
      appService.setFolders(original);
      appService.setMaxFolders(originalMax);
    }
  }

  public void testTrailingSlashKeepsFolderName() {
    myFixture.addFileToProject("docs/readme.md", "# readme");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("docs/"));

    GrailsCustomFolderNode node = existingFolderNode(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

    assertEquals("a trailing slash still produces its node", "docs", label(node));
    assertEquals("a trailing slash keeps the configured path as typed", "docs/",
                 node.getConfiguredPath());
    assertFalse("a trailing slash still resolves the existing folder", node.getChildren().isEmpty());
  }

  public void testLeadingDotSlashStillResolves() {
    myFixture.addFileToProject("docs/readme.md", "# readme");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("./docs"));

    GrailsCustomFolderNode node = existingFolderNode(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

    assertEquals("a leading ./ does not blank the folder name", "docs", label(node));
    assertFalse("a leading ./ still resolves the existing folder", node.getChildren().isEmpty());
  }

  public void testTrailingBackslashKeepsFolderName() {
    myFixture.addFileToProject("docs/readme.md", "# readme");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("docs\\"));

    GrailsCustomFolderNode node = existingFolderNode(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

    assertEquals("a trailing backslash does not blank the folder name", "docs", label(node));
    assertFalse("a trailing backslash still resolves the existing folder", node.getChildren().isEmpty());
  }

  public void testNodesSortedAlphabeticallyByLabel() {
    myFixture.addFileToProject("zebra/z.txt", "");
    myFixture.addFileToProject("alpha/a.txt", "");
    myFixture.addFileToProject("middle/m.txt", "");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("zebra", "alpha", "middle"));

    List<GrailsCustomFolder> nodes = customFolderNodes(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

    assertEquals("nodes are ordered alphabetically by label regardless of configuration order",
                 List.of("alpha", "middle", "zebra"),
                 nodes.stream().map(GrailsCustomFoldersNodeProviderTest::label).toList());
  }

  /**
   * Pinning that the sort key is the last path segment and not the configured path. Here the two
   * disagree — {@code b/docs} sorts before {@code a/zzz} by folder name and after it by path — so a
   * provider that sorted by anything else would silently reorder the pane while the labels still
   * looked right.
   */
  public void testNodesAreSortedByFolderNameNotByConfiguredPath() {
    myFixture.addFileToProject("b/docs/readme.md", "# readme");
    myFixture.addFileToProject("a/zzz/readme.md", "# readme");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("a/zzz", "b/docs"));

    List<GrailsCustomFolder> nodes = customFolderNodes(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

    assertEquals("the folder name is the sort key, not the configured path",
                 List.of("docs", "zzz"),
                 nodes.stream().map(GrailsCustomFoldersNodeProviderTest::label).toList());
  }

  public void testNonExistentFolderIsPlaceholder() {
    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("nonexistent"));

    GrailsMissingCustomFolderNode node = missingFolderNode(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

    assertEquals("the placeholder is labelled with the configured folder name", "nonexistent", label(node));
    assertTrue("a placeholder has no children", node.getChildren().isEmpty());
    assertEquals("a placeholder must report itself as a leaf, or the tree keeps asking for children",
                 LeafState.ALWAYS, node.getLeafState());
  }

  /**
   * The pane's visitor is what *Reveal in Project View* and *Select Opened File* drive, and it
   * answers from the node's value rather than from any {@code contains} the node writes itself. A
   * node whose value is a plain String has no PsiElement to match against and the platform answers
   * SKIP_CHILDREN, which is the whole defect the directory node split fixes.
   */
  public void testVisitorRevealsFileInsideCustomFolder() {
    myFixture.addFileToProject("docs/readme.md", "# readme");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("docs"));

    GrailsCustomFolderNode node = existingFolderNode(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));
    VirtualFile file = myFixture.findFileInTempDir("docs/readme.md");
    assertNotNull(file);

    assertEquals("the visitor must walk into the custom folder, which is what *Reveal* relies on",
                 TreeVisitor.Action.CONTINUE, reveal(node, file));
  }

  /** A placeholder stands for nothing, so the visitor must skip it instead of failing on it. */
  public void testVisitorSkipsPlaceholderForMissingFolder() {
    myFixture.addFileToProject("docs/readme.md", "# readme");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("nonexistent"));

    GrailsMissingCustomFolderNode node = missingFolderNode(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));
    VirtualFile file = myFixture.findFileInTempDir("docs/readme.md");
    assertNotNull(file);

    assertEquals("a missing folder must not claim to contain anything",
                 TreeVisitor.Action.SKIP_CHILDREN, reveal(node, file));
  }

  /**
   * The PsiDirectory value is what puts PSI_ELEMENT and VIRTUAL_FILE in the data context, which is
   * what *New &gt;*, *Delete*, *Find in Files* and *Copy Path* read on the node.
   */
  public void testExistingFolderNodeCarriesTheDirectoryAsValue() {
    myFixture.addFileToProject("docs/readme.md", "# readme");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("docs"));

    GrailsCustomFolderNode node = existingFolderNode(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

    Object value = node.getValue();
    assertTrue("the node's value is the folder's PsiDirectory", value instanceof PsiDirectory);
    assertEquals("the value is the configured folder itself", "docs", ((PsiDirectory)value).getName());
  }

  /**
   * The location string and the tooltip are both the configured path exactly as typed, which is what
   * keeps two spellings of the same directory — {@code docs} and {@code app/docs} — tellable apart in
   * the tree. Both are written outside the label, so only these two assertions notice if they go.
   */
  public void testExistingFolderNodeCarriesTheConfiguredPathAsLocationAndTooltip() {
    myFixture.addFileToProject("docs/readme.md", "# readme");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("./docs/"));

    GrailsCustomFolderNode node = existingFolderNode(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

    PresentationData presentation = rendered(node);
    assertEquals("the location string is the configured path exactly as typed",
                 "./docs/", presentation.getLocationString());
    assertEquals("the tooltip is the configured path", "./docs/", presentation.getTooltip());
    assertEquals("the label is the folder name, not the spelling it was configured with",
                 "docs", presentation.getPresentableText());
  }

  /**
   * The tree computes leaf state by calling getChildren(), so a placeholder that never settles leaves
   * the pane on "Loading...". An existing folder must stay expandable.
   */
  public void testExistingFolderIsNotALeaf() {
    myFixture.addFileToProject("docs/readme.md", "# readme");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("docs"));

    GrailsCustomFolderNode node = existingFolderNode(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

    assertEquals("an existing folder must stay expandable", LeafState.NEVER, node.getLeafState());
    assertFalse("an existing folder has children to browse", node.getChildren().isEmpty());
  }

  /**
   * {@code contains} here is the platform's own {@code ProjectViewNode.contains(VirtualFile)}, which
   * the directory node inherits: it is the override the pane's visitor consults. The placeholder has
   * no such method at all, which is why {@link #testVisitorSkipsPlaceholderForMissingFolder()} is
   * what pins what a missing folder contributes to a reveal.
   */
  public void testContainsFileInsideFolder() {
    myFixture.addFileToProject("docs/readme.md", "# readme");

    GrailsCustomFoldersProjectService service = getProject().getService(GrailsCustomFoldersProjectService.class);
    service.setFolders(List.of("docs"));

    GrailsCustomFolderNode node = existingFolderNode(
      new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT));

    VirtualFile file = myFixture.findFileInTempDir("docs/readme.md");
    assertNotNull(file);
    assertTrue("the folder directory itself is contained", node.contains(file.getParent()));
    assertTrue("a file inside the folder is contained, so Reveal in Project View can find the node",
               node.contains(file));
  }

  public void testNoNodesWhenNoFoldersConfigured() {
    withGlobalFolders(List.of(), () -> {
      getProject().getService(GrailsCustomFoldersProjectService.class).setFolders(List.of());

      Collection<AbstractTreeNode<?>> nodes =
        new GrailsCustomFoldersNodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT);

      assertTrue("no custom folder nodes without configured folders", customFolderNodes(nodes).isEmpty());
    });
  }

  /**
   * Drives the platform's own traversal, the way the pane does when it reveals a file: the visitor
   * is built from the file being revealed, then asked about the node. It runs off the EDT because
   * {@code AbstractTreeNodeVisitor.visit} asserts a background thread, and under a read action
   * because reading the PSI needs one.
   */
  private @NotNull TreeVisitor.Action reveal(@NotNull AbstractTreeNode<?> node, @NotNull VirtualFile file) {
    PsiFile psiFile = PsiManager.getInstance(getProject()).findFile(file);
    assertNotNull("the fixture must resolve the revealed file to a PsiFile", psiFile);

    Callable<TreeVisitor.Action> visit = () -> ApplicationManager.getApplication().runReadAction(
      (Computable<TreeVisitor.Action>)() -> AbstractProjectViewPane.createVisitor(psiFile, file)
        .visit(new TreePath(node)));
    try {
      return PlatformTestUtil.waitForFuture(
        ApplicationManager.getApplication().executeOnPooledThread(visit), 10_000);
    }
    catch (Exception e) {
      throw new IllegalStateException("the platform visitor could not be run off the EDT", e);
    }
  }

  private static @NotNull List<GrailsCustomFolder> customFolderNodes(
    @NotNull Collection<AbstractTreeNode<?>> nodes) {
    return nodes.stream().filter(GrailsCustomFolder.class::isInstance).map(GrailsCustomFolder.class::cast).toList();
  }

  private static @NotNull GrailsCustomFolderNode existingFolderNode(@NotNull Collection<AbstractTreeNode<?>> nodes) {
    for (AbstractTreeNode<?> node : nodes) {
      if (node instanceof GrailsCustomFolderNode folder) return folder;
    }
    throw new AssertionError("expected an existing custom folder node among " + nodes);
  }

  private static @NotNull GrailsMissingCustomFolderNode missingFolderNode(
    @NotNull Collection<AbstractTreeNode<?>> nodes) {
    for (AbstractTreeNode<?> node : nodes) {
      if (node instanceof GrailsMissingCustomFolderNode placeholder) return placeholder;
    }
    throw new AssertionError("expected a placeholder custom folder node among " + nodes);
  }

  private static @NotNull String label(@NotNull GrailsCustomFolder node) {
    if (node instanceof GrailsPsiDirectoryNode dir) return rendered(dir).getPresentableText();
    AbstractTreeNode<?> placeholder = (AbstractTreeNode<?>)node;
    placeholder.update();
    return placeholder.getPresentation().getPresentableText();
  }

}

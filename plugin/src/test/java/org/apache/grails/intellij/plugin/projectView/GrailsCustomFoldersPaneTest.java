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

package org.apache.grails.intellij.plugin.projectView;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.fileTypes.FileTypeRegistry;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.testFramework.LoggedErrorProcessor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.ui.tree.LeafState;
import com.intellij.util.ThrowableRunnable;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.config.GrailsConstants;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersProjectService;
import org.apache.grails.intellij.plugin.projectView.api.GrailsViewNodeProvider;
import org.apache.grails.intellij.plugin.projectView.impl.GrailsCustomFoldersNodeProvider;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsApplicationNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsCustomFolder;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsCustomFolderNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsMissingCustomFolderNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsRootNode;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.apache.grails.intellij.plugin.util.version.Version;
import org.jetbrains.annotations.NotNull;

import javax.swing.Icon;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;


/**
 * The Grails pane builds its tree by calling {@code getChildren()} down the node chain on a
 * background thread, so any exception a node throws aborts the whole repaint and blanks the view.
 * Applying a custom-folder setting repaints the pane, which is how a folder that does not exist came
 * to be reported as "the plugin fails to repaint". These tests walk the real node chain.
 */
public class GrailsCustomFoldersPaneTest extends GrailsTestCase {

  private static final String LOGGER_CATEGORY = GrailsApplicationNode.class.getName();

  /**
   * A configured folder that does not exist on disk must still produce a node, and building the tree
   * around it must not throw.
   */
  public void testTreeBuildsWithNonExistentFolder() {
    addGrailsApp();
    List<String> paths = buildTreeAndCollectCustomFolderPaths(List.of("no-such-directory"));

    assertTrue("a non-existent folder should still get its node",
                paths.contains("no-such-directory"));
  }

  /**
   * An absolute path is no longer special-cased: the provider reads it as a relative path and resolves
   * it against the application root, so it names nothing that exists. It still gets its placeholder
   * node, because a configured path that resolves to nothing must never disappear from the view.
   */
  public void testTreeBuildsWithNonExistentAbsoluteFolder() {
    addGrailsApp();
    List<String> paths = buildTreeAndCollectCustomFolderPaths(List.of("/no-such-home/aarone"));

    assertTrue("a non-existent absolute folder should still get its node",
                paths.contains("/no-such-home/aarone"));

    Collection<AbstractTreeNode<?>> applicationNodes = buildApplicationNodesFor(List.of("/no-such-home/aarone"));
    AbstractTreeNode<?> node = customFolderNode(applicationNodes, "/no-such-home/aarone");
    assertTrue("the path is read as relative and resolved against the application root, which it misses",
               node instanceof GrailsMissingCustomFolderNode);
    assertEquals("a folder that does not exist has nothing to expand",
                 LeafState.ALWAYS, node.getLeafState());
  }

  /** Neither an unresolvable path nor a missing parent may suppress the node. */
  public void testTreeBuildsWhenFolderParentIsMissing() {
    addGrailsApp();
    List<String> paths = buildTreeAndCollectCustomFolderPaths(List.of("missing-parent/missing-child"));

    assertTrue("a folder with a missing parent should still get its placeholder node",
                paths.contains("missing-parent/missing-child"));
  }

  /**
   * An existing folder and a missing one both survive, and both appear: an unresolvable path must
   * not take its siblings down with it.
   */
  public void testExistingAndNonExistentFoldersBothBuild() {
    addGrailsApp();
    myFixture.addFileToProject("docs/readme.md", "# readme");

    List<String> paths = buildTreeAndCollectCustomFolderPaths(List.of("docs", "no-such-directory"));

    assertTrue("the existing folder should be rendered", paths.contains("docs"));
    assertTrue("the non-existent folder should be rendered alongside it", paths.contains("no-such-directory"));
  }

  /**
   * A missing {@code grails-app} made {@code GrailsApplicationBase.getAppRoot()} throw while the
   * pane built children, and that exception aborted the whole repaint. The providers must tolerate
   * it, so a custom folder node is still produced.
   *
   * <p>Driven through a stub rather than the fixture, because the fixture's application is filtered
   * out of the tree once grails-app is gone, which would hide the very provider call under test.
   */
  public void testProvidersTolerateMissingGrailsAppDirectory() {
    addGrailsApp();
    getProject().getService(GrailsCustomFoldersProjectService.class).setFolders(List.of("no-such-directory"));

    GrailsApplication application = new MissingAppRootApplication(getProject());
    // Every registered provider must survive, because one throwing provider aborts the pane repaint.
    for (GrailsViewNodeProvider provider : GrailsViewNodeProvider.EP_NAME.getExtensionList()) {
      provider.createNodes(application, ViewSettings.DEFAULT);
    }

    List<String> paths = collectCustomFolderPaths(
      new GrailsCustomFoldersNodeProvider().createNodes(application, ViewSettings.DEFAULT));

    assertTrue("a missing grails-app must not prevent custom folder nodes",
                paths.contains("no-such-directory"));
  }

  /**
   * A provider that fails with a plain exception must not cost the other providers their nodes.
   *
   * <p>Registered as an extension so it runs inside {@code GrailsApplicationNode.getChildren()},
   * which is where the isolation lives.
   */
  public void testThrowingProviderDoesNotSuppressSiblingNodes() {
    registerProvider(new ThrowingNodeProvider(new IllegalStateException("provider blew up")));
    assertCustomFolderNodeSurvives("no-such-directory");
  }

  /**
   * A provider that fails is reported as an error on this node's logger, through the platform's error
   * processor. A {@code warn} surfaces as an IDE error-report dialog instead, so the level is part of
   * what the isolation try/catch owes the platform.
   */
  public void testFailingProviderIsLoggedAsError() {
    registerProvider(new ThrowingNodeProvider(new IllegalStateException("provider blew up")));
    configureCustomFolder("no-such-directory");

    RecordingLoggedErrorProcessor processor = new RecordingLoggedErrorProcessor();
    LoggedErrorProcessor.executeWith(processor, (ThrowableRunnable<RuntimeException>)() -> {
      GrailsApplicationNode node = new GrailsApplicationNode(singleApplication(), ViewSettings.DEFAULT);
      collectCustomFolderPaths(node.getChildren());
    });

    assertEquals("a failing provider must be logged exactly once: " + processor.myRecords,
                 1, processor.myRecords.size());
    assertTrue("expected an ERROR, got: " + processor.myRecords.get(0),
               processor.myRecords.get(0).startsWith("ERROR "));
  }

  /**
   * The platform cancels a background read by throwing a control-flow exception, and retries the
   * build afterwards. Swallowing that would leave the pane on "Loading..." forever, so it must be
   * rethrown rather than treated as a provider failure.
   */
  public void testControlFlowCancellationIsRethrown() {
    registerProvider(new ThrowingNodeProvider(new ProcessCanceledException()));
    GrailsApplicationNode node = new GrailsApplicationNode(singleApplication(), ViewSettings.DEFAULT);
    try {
      node.getChildren();
      fail("a cancelled background read must be rethrown, not swallowed");
    }
    catch (ProcessCanceledException expected) {
      // Rethrown untouched, so the platform can retry the build. Logging it instead throws a
      // different exception out of the logger, which still strands the pane on "Loading...".
    }
  }

  /**
   * FR48's {@code .github} fixture, observed through the node chain the pane builds rather than
   * reasoned about: a hidden dot-directory inside the application renders as a real, expandable
   * folder node — not as the placeholder, and not filtered away.
   *
   * <p>This is the rule the README and {@code IMPROVEMENT-PLAN.md} §2.8 state, and the one §2.8
   * previously got wrong: the platform's default {@code Ignored Files and Folders} list matches named
   * entries and glob suffixes, and carries no rule covering dot-directories. Both halves are asserted
   * here, so neither the README nor §2.8 can drift back to an unverified generalisation.
   */
  public void testHiddenDotDirectoryRendersAsAnExpandableFolderNode() {
    myFixture.addFileToProject(".github/workflows/ci.yml", "jobs: {}");
    myFixture.addFileToProject(".git/config", "[core]");

    Collection<AbstractTreeNode<?>> applicationNodes = buildApplicationNodesFor(List.of(".github"));
    AbstractTreeNode<?> node = customFolderNode(applicationNodes, ".github");

    assertTrue("an existing .github must get the navigable node, not the placeholder",
                node instanceof GrailsCustomFolderNode);
    assertTrue("the node's value is the directory, which is what makes it traversable",
                ((GrailsCustomFolderNode)node).getValue() instanceof PsiDirectory);
    assertEquals(".github is labelled with its folder name, dots and all", ".github", renderedLabel(node));
    assertFalse(".github has content, so the folder must stay expandable", node.getChildren().isEmpty());

    VirtualFile fixtureRoot = myFixture.getTempDirFixture().getFile("");
    assertNotNull(fixtureRoot);
    assertFalse(".github must not be ignored by the platform's default list",
                isIgnored(fixtureRoot.findChild(".github")));
    assertTrue(".git is the entry that list actually contains",
               isIgnored(fixtureRoot.findChild(".git")));
  }

  /**
   * FR48's absolute path, observed the same way: a directory that exists on disk but lies outside the
   * Grails application is not resolved. The path is read relative to the application root, so it names
   * nothing that exists and the pane renders the placeholder. That is the shape of the whole "no
   * external directories" limitation, recorded here instead of being asserted twice in the docs.
   */
  public void testAbsolutePathOutsideTheApplicationRendersAsPlaceholder() {
    // An absolute directory that exists on this machine and sits outside the application: /tmp does,
    // and no relative reading of it can land inside the application root.
    String outside = new java.io.File(System.getProperty("java.io.tmpdir")).getAbsolutePath();
    assertTrue("the recorded outside directory must be absolute: " + outside, outside.startsWith("/"));

    Collection<AbstractTreeNode<?>> applicationNodes = buildApplicationNodesFor(List.of(outside));
    AbstractTreeNode<?> node = customFolderNode(applicationNodes, outside);

    assertTrue("an absolute path resolves to nothing and keeps its placeholder",
                node instanceof GrailsMissingCustomFolderNode);
    assertEquals("a folder that does not exist has nothing to expand",
                  LeafState.ALWAYS, node.getLeafState());
    assertTrue("a placeholder has no children", node.getChildren().isEmpty());
  }

  /** The label the renderer shows, read through {@code update()} so decorators and postprocess run. */
  private static @NotNull String renderedLabel(@NotNull AbstractTreeNode<?> node) {
    node.update();
    return node.getPresentation().getPresentableText();
  }

  private static boolean isIgnored(@NotNull VirtualFile file) {
    return FileTypeRegistry.getInstance().isFileIgnored(file);
  }

  /** Applies one folder and asserts its node is still built through the application's children. */
  private void assertCustomFolderNodeSurvives(@NotNull String folder) {
    configureCustomFolder(folder);

    // The provider failure is now logged at error level, which the test framework reports as a test
    // failure unless a processor swallows it; the node surviving it is what this test asserts.
    LoggedErrorProcessor.executeWith(new RecordingLoggedErrorProcessor(), (ThrowableRunnable<RuntimeException>)() -> {
      GrailsApplicationNode node = new GrailsApplicationNode(singleApplication(), ViewSettings.DEFAULT);

      assertTrue("a failing provider must not remove the custom folder node",
                 collectCustomFolderPaths(node.getChildren()).contains(folder));
    });
  }

  /** Applies one folder to the application the pane will build its children for. */
  private void configureCustomFolder(@NotNull String folder) {
    addGrailsApp();
    createApplicationProperties();
    PlatformTestUtil.waitForFuture(GrailsApplicationManager.getInstance(getProject()).queueUpdate(), 5_000);
    getProject().getService(GrailsCustomFoldersProjectService.class).setFolders(List.of(folder));
  }

  private void registerProvider(@NotNull GrailsViewNodeProvider provider) {
    GrailsViewNodeProvider.EP_NAME.getPoint().registerExtension(provider, getTestRootDisposable());
  }

  /** A provider that always throws, to exercise the isolation in the application node. */
  private static final class ThrowingNodeProvider implements GrailsViewNodeProvider {

    private final @NotNull RuntimeException myFailure;

    private ThrowingNodeProvider(@NotNull RuntimeException failure) {
      myFailure = failure;
    }

    @Override
    public @NotNull Collection<AbstractTreeNode<?>> createNodes(@NotNull GrailsApplication application,
                                                                @NotNull ViewSettings settings) {
      throw myFailure;
    }
  }

  /**
   * Records what reaches the {@code GrailsApplicationNode} logger, so a provider failure can be
   * asserted to be reported as an error rather than as a warning.
   */
  private static final class RecordingLoggedErrorProcessor extends LoggedErrorProcessor {

    private final @NotNull List<String> myRecords = new ArrayList<>();

    @Override
    public @NotNull Set<Action> processError(@NotNull String category, @NotNull String message, String[] details, Throwable t) {
      record(category, "ERROR " + message);
      return EnumSet.noneOf(Action.class);
    }

    @Override
    public boolean processWarn(@NotNull String category, @NotNull String message, Throwable t) {
      record(category, "WARN " + message);
      return false;
    }

    private void record(@NotNull String category, @NotNull String record) {
      // Logger.getInstance(Class) registers the category with a leading '#', so match on the suffix.
      if (category.endsWith(LOGGER_CATEGORY)) {
        myRecords.add(record);
      }
    }
  }

  /**
   * Stands in for an application whose {@code grails-app} is gone. {@code getAppRoot()} throws the
   * same NPE {@code GrailsApplicationBase} throws in that state, without needing the fixture's
   * application to be invalidated (which would remove it from the tree before the providers run).
   */
  private static final class MissingAppRootApplication extends UserDataHolderBase implements GrailsApplication {

    private final @NotNull Project myProject;
    private final @NotNull VirtualFile myRoot;

    private MissingAppRootApplication(@NotNull Project project) {
      myProject = project;
      myRoot = project.getBaseDir();
    }

    @Override
    public @NotNull VirtualFile getRoot() {
      return myRoot;
    }

    @Override
    public @NotNull VirtualFile getAppRoot() {
      throw new NullPointerException("grails-app is missing, as GrailsApplicationBase.getAppRoot() does");
    }

    @Override
    public @NotNull Project getProject() {
      return myProject;
    }

    @Override
    public @NotNull String getName() {
      return "noAppRoot";
    }

    @Override
    public @NotNull Icon getIcon() {
      return AllIcons.Nodes.Package;
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

  /**
   * Builds the node chain the pane builds: root, then each application's children, which is where
   * every node provider runs.
   */
  private @NotNull List<String> buildTreeAndCollectCustomFolderPaths(@NotNull List<String> folders) {
    Collection<AbstractTreeNode<?>> applicationNodes = buildApplicationNodesFor(folders);
    assertTrue("the fixture must yield at least one application node, so the tree really was built",
               !applicationNodes.isEmpty());
    return collectCustomFolderPaths(applicationNodes);
  }

  private @NotNull Collection<AbstractTreeNode<?>> buildApplicationNodesFor(@NotNull List<String> folders) {
    createApplicationProperties();
    PlatformTestUtil.waitForFuture(GrailsApplicationManager.getInstance(getProject()).queueUpdate(), 5_000);
    singleApplication();
    getProject().getService(GrailsCustomFoldersProjectService.class).setFolders(folders);
    return buildApplicationNodes();
  }

  /** The application's child nodes, as the pane builds them: every node provider runs here. */
  private @NotNull Collection<AbstractTreeNode<?>> buildApplicationNodes() {
    GrailsRootNode root = new GrailsRootNode(getProject(), ViewSettings.DEFAULT);
    Collection<AbstractTreeNode<?>> nodes = new ArrayList<>();
    for (AbstractTreeNode<?> applicationNode : root.getChildren()) {
      if (applicationNode instanceof GrailsApplicationNode) {
        nodes.addAll(applicationNode.getChildren());
      }
    }
    return nodes;
  }

  /** The node for one configured path, so the node kind and its leaf state can be pinned. */
  private static @NotNull AbstractTreeNode<?> customFolderNode(@NotNull Collection<AbstractTreeNode<?>> nodes,
                                                               @NotNull String configuredPath) {
    for (AbstractTreeNode<?> node : nodes) {
      if (node instanceof GrailsCustomFolder folder && configuredPath.equals(folder.getConfiguredPath())) {
        return node;
      }
    }
    throw new AssertionError("no custom folder node for " + configuredPath + " among " + nodes);
  }

  private static @NotNull List<String> collectCustomFolderPaths(@NotNull Collection<AbstractTreeNode<?>> nodes) {
    List<String> paths = new ArrayList<>();
    for (AbstractTreeNode<?> node : nodes) {
      if (node instanceof GrailsCustomFolder folder) {
        paths.add(folder.getConfiguredPath());
      }
    }
    return paths;
  }

  private @NotNull GrailsApplication singleApplication() {
    Collection<GrailsApplication> applications = GrailsApplicationManager.getInstance(getProject()).getApplications();
    assertTrue("fixture must register a Grails application", !applications.isEmpty());
    return applications.iterator().next();
  }

  private void addGrailsApp() {
    myFixture.addFileToProject("grails-app/i18n/messages.properties", "greeting=hello");
  }

  /**
   * {@code TestGrailsApplicationProvider} only reports an application that has
   * {@code application.properties}, which an earlier test in the same fixture may have written
   * already. It is a <em>file</em>, so it is created through the fixture's file API and its content
   * replaced through {@code saveText}; resolving the path with the directory API creates a directory
   * of that name and then writes content into a directory.
   */
  private void createApplicationProperties() {
    VirtualFile properties = myFixture.getTempDirFixture().getFile("application.properties");
    if (properties == null) {
      properties = myFixture.getTempDirFixture().createFile("application.properties");
    }
    myFixture.saveText(properties, "app.grails.version=7.0.0\napp.name=TestApp\n");
  }
}

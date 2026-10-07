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

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.config.GrailsConstants;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsApplicationNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsPsiDirectoryNode;
import org.apache.grails.intellij.plugin.projectView.nodes.OtherGrailsAppSourcesNode;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.util.version.Version;

import java.util.Collection;
import javax.swing.Icon;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public abstract class GrailsNodeProviderTestSupport extends GrailsTestCase {

  protected void addAssetAndTranslationFixture() {
    myFixture.addFileToProject("grails-app/i18n/messages.properties", "greeting=hello");
    myFixture.addFileToProject("grails-app/i18n/messages_de.properties", "greeting=hallo");
    myFixture.addFileToProject("grails-app/assets/stylesheets/app.css", "h1 { color: red; }");
    myFixture.addFileToProject("grails-app/assets/images/logo.png", "fake-png");
    myFixture.addFileToProject("grails-app/assets/javascripts/app.js", "console.log(1);");
    myFixture.addFileToProject("grails-app/assets/extra.txt", "stray");
    myFixture.addFileToProject("grails-app/utils/ShoutyCodec.groovy", "class ShoutyCodec { static encode = { 'A' } }");
    myFixture.addFileToProject("grails-app/migrations/changelog.groovy", "databaseChangeLog { }");
  }

  protected @NotNull GrailsApplication testApplication(boolean grailsAppSubfolder) {
    VirtualFile root = myFixture.getTempDirFixture().getFile("");
    assertNotNull(root);
    return new TestGrailsApplication(root, getProject(), grailsAppSubfolder);
  }

  protected static @Nullable GrailsPsiDirectoryNode findNode(@NotNull Collection<AbstractTreeNode<?>> nodes,
                                                            @NotNull String name) {
    for (AbstractTreeNode<?> node : nodes) {
      if (node instanceof GrailsPsiDirectoryNode dirNode && name.equals(dirNode.getValue().getName())) {
        return dirNode;
      }
    }
    return null;
  }

  protected @NotNull OtherGrailsAppSourcesNode otherSourcesNode(@NotNull GrailsApplication application) {
    PsiDirectory appRoot = PsiManager.getInstance(application.getProject()).findDirectory(application.getAppRoot());
    assertNotNull(appRoot);
    OtherGrailsAppSourcesNode node = new OtherGrailsAppSourcesNode(appRoot, ViewSettings.DEFAULT);
    node.setParent(new GrailsApplicationNode(application, ViewSettings.DEFAULT));
    return node;
  }

  /** The node's title and location string are what the tree shows, not the directory name. */
  protected static void assertTitleAndLocation(@Nullable GrailsPsiDirectoryNode node,
                                               @NotNull String expectedTitle,
                                               @NotNull String expectedLocation) {
    assertNotNull(expectedTitle + " node must be present", node);

    PresentationData data = rendered(node);

    assertEquals("the node carries the label its directory maps to", expectedTitle, data.getPresentableText());
    assertEquals("a retitled node must keep its directory discoverable through its location string",
                 expectedLocation, data.getLocationString());
  }

  /**
   * Reads a node the way the renderer does: {@code updateImpl} then {@code postprocess}. The second call
   * matters, because {@code PsiDirectoryNode} writes the directory name into the presentation during
   * {@code updateImpl} — as coloured fragments when the directory is a module content root, and otherwise
   * via {@code setPresentableText}, taking the qualified name from
   * {@code ProjectViewDirectoryHelper.getNodeName} — and {@code GrailsPsiDirectoryNode} only replaces it
   * afterwards.
   */
  protected static @NotNull PresentationData rendered(@NotNull GrailsPsiDirectoryNode node) {
    // update() runs updateImpl and then postprocess, which is the sequence the renderer sees; getPresentation()
    // then returns that result. postprocess is protected, so it cannot be called from this package directly.
    node.update();
    return node.getPresentation();
  }

  protected static void assertTitleAndIcon(@Nullable GrailsPsiDirectoryNode node,
                                           @NotNull String expectedTitle,
                                           @NotNull Icon expectedIcon) {
    assertNotNull(expectedTitle + " node must be present", node);

    PresentationData data = rendered(node);

    assertEquals(expectedTitle, data.getPresentableText());
    assertSame(expectedIcon, data.getIcon(false));
  }

  protected static @NotNull PsiDirectoryNode assetsNodeUnderOtherSources(@NotNull Collection<AbstractTreeNode<?>> nodes) {
    PsiDirectoryNode assetsNode = findAssetsNode(nodes);
    assertNotNull("assets must remain under Other sources for stray files", assetsNode);
    return assetsNode;
  }

  protected static @Nullable PsiDirectoryNode findAssetsNode(@NotNull Collection<AbstractTreeNode<?>> nodes) {
    for (AbstractTreeNode<?> node : nodes) {
      if (node instanceof PsiDirectoryNode dirNode && GrailsViewItems.ASSETS_DIR.equals(dirNode.getValue().getName())) {
        return dirNode;
      }
    }
    return null;
  }

  protected static final class TestGrailsApplication extends UserDataHolderBase implements GrailsApplication {

    private final @NotNull VirtualFile myRoot;
    private final @NotNull VirtualFile myAppRoot;
    private final @NotNull Project myProject;

    protected TestGrailsApplication(@NotNull VirtualFile root, @NotNull Project project, boolean grailsAppSubfolder) {
      myRoot = root;
      myProject = project;
      // Grails 4+ nests the app sources under grails-app/ (GrailsAppNodeProviderTest); the Grails 3
      // layout has them at the project root (Grails3NodeProviderTest).
      myAppRoot = grailsAppSubfolder ? root.findChild(GrailsConstants.APP_DIRECTORY) : root;
      assertNotNull("test fixture must contain " + GrailsConstants.APP_DIRECTORY, myAppRoot);
    }

    @Override
    public @NotNull String getName() {
      return "testApp";
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
      return myAppRoot;
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
      return GlobalSearchScope.allScope(getProject());
    }
  }
}

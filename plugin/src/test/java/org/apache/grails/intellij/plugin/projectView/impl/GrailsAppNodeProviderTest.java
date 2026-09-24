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
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.PsiDirectoryNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileSystemItemFilter;
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
import org.apache.grails.intellij.plugin.artefact.impl.ControllerArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.impl.DomainArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.impl.InterceptorArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.impl.ServiceArtefactHandler;
import org.apache.grails.intellij.plugin.artefact.impl.TaglibArtefactHandler;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsApplicationNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsArtefactHandlerNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsPsiDirectoryNode;
import org.apache.grails.intellij.plugin.projectView.nodes.OtherGrailsAppSourcesNode;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.util.version.Version;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.Icon;

public class GrailsAppNodeProviderTest extends GrailsTestCase {

  public void testRendersTranslationsAndAssetSubfolderNodes() {
    addAssetAndTranslationFixture();

    Collection<AbstractTreeNode<?>> nodes = new GrailsAppNodeProvider().createNodes(testApplication(), ViewSettings.DEFAULT);

    GrailsPsiDirectoryNode translations = findNode(nodes, "i18n");
    assertNotNull("Translations node must be present", translations);
    assertEquals(NodeWeights.TRANSLATIONS_FOLDER, translations.getNodeWeight());
    assertSame(AllIcons.FileTypes.Properties, translations.getNodeIcon());

    GrailsPsiDirectoryNode stylesheets = findNode(nodes, "stylesheets");
    assertNotNull("Stylesheets node must be present", stylesheets);
    assertEquals(NodeWeights.STYLESHEETS_FOLDER, stylesheets.getNodeWeight());
    assertSame(AllIcons.FileTypes.Css, stylesheets.getNodeIcon());

    GrailsPsiDirectoryNode images = findNode(nodes, "images");
    assertNotNull("Images node must be present", images);
    assertEquals(NodeWeights.IMAGES_FOLDER, images.getNodeWeight());
    assertSame(AllIcons.FileTypes.Image, images.getNodeIcon());

    GrailsPsiDirectoryNode javascripts = findNode(nodes, "javascripts");
    assertNotNull("JavaScripts node must be present", javascripts);
    assertEquals(NodeWeights.JAVASCRIPTS_FOLDER, javascripts.getNodeWeight());
    assertSame(AllIcons.FileTypes.JavaScript, javascripts.getNodeIcon());
  }

  public void testMissingAssetSubfolderProducesNoNode() {
    myFixture.addFileToProject("grails-app/i18n/messages.properties", "a=b");
    myFixture.addFileToProject("grails-app/assets/stylesheets/app.css", "h1 {}");

    Collection<AbstractTreeNode<?>> nodes = new GrailsAppNodeProvider().createNodes(testApplication(), ViewSettings.DEFAULT);

    assertNotNull("Stylesheets node must be present", findNode(nodes, "stylesheets"));
    assertNull("no Images node when assets/images is absent (FR7)", findNode(nodes, "images"));
    assertNull("no JavaScripts node when assets/javascripts is absent (FR7)", findNode(nodes, "javascripts"));
    assertNull("no fake fonts node", findNode(nodes, "fonts"));
  }

  public void testOtherGrailsAppSourcesNodeExcludesTranslationsAndAssetSubfolders() {
    addAssetAndTranslationFixture();

    GrailsApplication application = testApplication();
    PsiDirectory appRoot = PsiManager.getInstance(application.getProject()).findDirectory(application.getAppRoot());
    assertNotNull(appRoot);

    OtherGrailsAppSourcesNode node = new OtherGrailsAppSourcesNode(appRoot, ViewSettings.DEFAULT);
    node.setParent(new GrailsApplicationNode(application, ViewSettings.DEFAULT));

    Collection<AbstractTreeNode<?>> children = node.getChildrenImpl();
    Set<String> childNames = children.stream()
      .map(child -> child.getValue() instanceof PsiDirectory directory
                    ? directory.getName() : String.valueOf(child.getValue()))
      .collect(Collectors.toSet());
    assertFalse("i18n must not duplicate under Other sources", childNames.contains("i18n"));

    AbstractTreeNode<?> assetsChild = children.stream()
      .filter(child -> child instanceof PsiDirectoryNode)
      .map(child -> (PsiDirectoryNode)child)
      .filter(child -> "assets".equals(child.getValue().getName()))
      .findFirst()
      .orElse(null);
    assertNotNull("assets must remain under Other sources for stray files", assetsChild);

    PsiFileSystemItemFilter filter = ((PsiDirectoryNode)assetsChild).getFilter();
    assertNotNull("assets node must carry a filter hiding the dedicated subfolders", filter);
    PsiDirectory assetsDir = ((PsiDirectoryNode)assetsChild).getValue();
    assertFalse("stylesheets subfolder hidden (FR4)", filter.shouldShow(assetsDir.findSubdirectory("stylesheets")));
    assertFalse("images subfolder hidden (FR4)", filter.shouldShow(assetsDir.findSubdirectory("images")));
    assertFalse("javascripts subfolder hidden (FR4)", filter.shouldShow(assetsDir.findSubdirectory("javascripts")));
    assertTrue("stray file directly under assets stays visible (FR8)", filter.shouldShow(assetsDir.findFile("extra.txt")));
  }

  public void testContainsExcludesHiddenAssetSubfoldersAndSpecialFolders() {
    addAssetAndTranslationFixture();
    myFixture.addFileToProject("grails-app/views/index.gsp", "<html/>");
    myFixture.addFileToProject("grails-app/assets/fonts/webfont.woff", "stray-font");

    GrailsApplication application = testApplication();
    PsiDirectory appRoot = PsiManager.getInstance(application.getProject()).findDirectory(application.getAppRoot());
    assertNotNull(appRoot);

    OtherGrailsAppSourcesNode node = new OtherGrailsAppSourcesNode(appRoot, ViewSettings.DEFAULT);
    node.setParent(new GrailsApplicationNode(application, ViewSettings.DEFAULT));

    assertFalse("css under a hidden asset subfolder must not be claimed (reveal dead-end)",
                node.contains(myFixture.findFileInTempDir("grails-app/assets/stylesheets/app.css")));
    assertFalse("messages under the extracted Translations folder must not be claimed",
                node.contains(myFixture.findFileInTempDir("grails-app/i18n/messages.properties")));
    assertFalse("views are rendered as a dedicated node, not here",
                node.contains(myFixture.findFileInTempDir("grails-app/views/index.gsp")));
    assertTrue("stray files directly under assets stay visible (FR8)",
               node.contains(myFixture.findFileInTempDir("grails-app/assets/extra.txt")));
    assertTrue("non-hidden asset subfolders stay visible",
               node.contains(myFixture.findFileInTempDir("grails-app/assets/fonts/webfont.woff")));
  }

  public void testHandlerWeightOrderForComparator() {
    Project project = getProject();
    ViewSettings settings = ViewSettings.DEFAULT;
    GrailsNodeComparator comparator = new GrailsNodeComparator(project, "GrailsView");

    GrailsArtefactHandlerNode domains = handlerNode(project, DomainArtefactHandler.INSTANCE, settings);
    GrailsArtefactHandlerNode services = handlerNode(project, ServiceArtefactHandler.INSTANCE, settings);
    GrailsArtefactHandlerNode controllers = handlerNode(project, ControllerArtefactHandler.INSTANCE, settings);
    GrailsArtefactHandlerNode interceptors = handlerNode(project, InterceptorArtefactHandler.INSTANCE, settings);
    GrailsArtefactHandlerNode taglibs = handlerNode(project, TaglibArtefactHandler.INSTANCE, settings);

    assertTrue("Domains must sort before Services", comparator.compare(domains, services) < 0);
    assertTrue("Services must sort before Controllers (reorder)", comparator.compare(services, controllers) < 0);
    assertTrue("Controllers must sort before Interceptors", comparator.compare(controllers, interceptors) < 0);
    assertTrue("Interceptors must sort before Tag Libraries", comparator.compare(interceptors, taglibs) < 0);
    assertTrue("Tag Libraries must sort last", comparator.compare(taglibs, domains) > 0);
  }

  public void testDirectoryWeightOrderForComparator() {
    addAssetAndTranslationFixture();
    myFixture.addFileToProject("grails-app/views/index.gsp", "<html/>");
    myFixture.addFileToProject("grails-app/conf/application.yml", "grails: {}");

    GrailsApplication application = testApplication();
    PsiDirectory appRoot = PsiManager.getInstance(getProject()).findDirectory(application.getAppRoot());
    assertNotNull(appRoot);
    PsiDirectory assets = appRoot.findSubdirectory("assets");
    assertNotNull(assets);

    Project project = getProject();
    GrailsNodeComparator comparator = new GrailsNodeComparator(project, "GrailsView");

    GrailsPsiDirectoryNode images = directoryNode(assets.findSubdirectory("images"), NodeWeights.IMAGES_FOLDER);
    GrailsPsiDirectoryNode javascripts = directoryNode(assets.findSubdirectory("javascripts"), NodeWeights.JAVASCRIPTS_FOLDER);
    GrailsPsiDirectoryNode stylesheets = directoryNode(assets.findSubdirectory("stylesheets"), NodeWeights.STYLESHEETS_FOLDER);
    GrailsPsiDirectoryNode views = directoryNode(appRoot.findSubdirectory("views"), NodeWeights.VIEWS_FOLDER);
    GrailsPsiDirectoryNode translations = directoryNode(appRoot.findSubdirectory("i18n"), NodeWeights.TRANSLATIONS_FOLDER);
    GrailsPsiDirectoryNode conf = directoryNode(appRoot.findSubdirectory("conf"), NodeWeights.CONFIG_FOLDER);

    assertTrue("Images must sort before JavaScripts", comparator.compare(images, javascripts) < 0);
    assertTrue("JavaScripts must sort before Stylesheets", comparator.compare(javascripts, stylesheets) < 0);
    assertTrue("Stylesheets must sort before Views", comparator.compare(stylesheets, views) < 0);
    assertTrue("Views must sort before Translations", comparator.compare(views, translations) < 0);
    assertTrue("Translations must sort before Configuration", comparator.compare(translations, conf) < 0);
  }

  private static GrailsPsiDirectoryNode directoryNode(@NotNull PsiDirectory dir, int weight) {
    return new GrailsPsiDirectoryNode(dir, ViewSettings.DEFAULT, weight);
  }

  private static GrailsArtefactHandlerNode handlerNode(@NotNull Project project,
                                                       @NotNull org.apache.grails.intellij.plugin.artefact.api.GrailsDisplayableArtefactHandler handler,
                                                       @NotNull ViewSettings settings) {
    return new GrailsArtefactHandlerNode(project, handler, settings);
  }

  private void addAssetAndTranslationFixture() {
    myFixture.addFileToProject("grails-app/i18n/messages.properties", "greeting=hello");
    myFixture.addFileToProject("grails-app/i18n/messages_de.properties", "greeting=hallo");
    myFixture.addFileToProject("grails-app/assets/stylesheets/app.css", "h1 { color: red; }");
    myFixture.addFileToProject("grails-app/assets/images/logo.png", "fake-png");
    myFixture.addFileToProject("grails-app/assets/javascripts/app.js", "console.log(1);");
    myFixture.addFileToProject("grails-app/assets/extra.txt", "stray");
  }

  private @NotNull GrailsApplication testApplication() {
    VirtualFile root = myFixture.getTempDirFixture().getFile("");
    assertNotNull(root);
    return new TestGrailsApplication(root, getProject());
  }

  private static final class TestGrailsApplication extends UserDataHolderBase implements GrailsApplication {

    private final @NotNull VirtualFile myRoot;
    private final @NotNull Project myProject;

    private TestGrailsApplication(@NotNull VirtualFile root, @NotNull Project project) {
      myRoot = root;
      myProject = project;
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
      return myRoot.findChild("grails-app");
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

  private static @Nullable GrailsPsiDirectoryNode findNode(@NotNull Collection<AbstractTreeNode<?>> nodes,
                                                           @NotNull String name) {
    for (AbstractTreeNode<?> node : nodes) {
      if (node instanceof GrailsPsiDirectoryNode dirNode && name.equals(dirNode.getValue().getName())) {
        return dirNode;
      }
    }
    return null;
  }
}
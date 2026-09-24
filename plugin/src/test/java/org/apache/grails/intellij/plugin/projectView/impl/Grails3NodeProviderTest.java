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
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.util.PlatformIcons;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsPsiDirectoryNode;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.util.version.Version;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import javax.swing.Icon;

public class Grails3NodeProviderTest extends GrailsTestCase {

  public void testSeparatesTestSourceRootsFromSrcNode() {
    myFixture.addFileToProject("src/test/groovy/SomeServiceSpec.groovy", "class SomeServiceSpec {}");
    myFixture.addFileToProject("src/integration-test/groovy/SomeServiceIT.groovy", "class SomeServiceIT {}");
    myFixture.addFileToProject("src/main/groovy/SomeService.groovy", "class SomeService {}");

    Collection<AbstractTreeNode<?>> nodes =
      new Grails3NodeProvider().createNodes(testApplication(), ViewSettings.DEFAULT);

    GrailsPsiDirectoryNode srcNode = findNode(nodes, "src");
    assertNotNull("src node must be present", srcNode);
    assertEquals(NodeWeights.SRC_FOLDERS, srcNode.getNodeWeight());
    assertNotNull("src node must have a filter that hides test roots", srcNode.getFilter());

    PsiDirectory srcDir = srcNode.getValue();
    PsiDirectory mainDir = srcDir.findSubdirectory("main");
    PsiDirectory testDir = srcDir.findSubdirectory("test");
    PsiDirectory integrationTestDir = srcDir.findSubdirectory("integration-test");
    assertNotNull(mainDir);
    assertNotNull(testDir);
    assertNotNull(integrationTestDir);

    assertTrue("plain sources keep showing under src", srcNode.getFilter().shouldShow(mainDir));
    assertFalse("unit test root must not duplicate under src", srcNode.getFilter().shouldShow(testDir));
    assertFalse("integration test root must not duplicate under src", srcNode.getFilter().shouldShow(integrationTestDir));

    Set<String> nodeNames = nodes.stream()
      .filter(GrailsPsiDirectoryNode.class::isInstance)
      .map(GrailsPsiDirectoryNode.class::cast)
      .map(node -> node.getValue().getName())
      .collect(Collectors.toSet());
    assertTrue("top-level nodes must contain src, got " + nodeNames, nodeNames.contains("src"));
    assertTrue("top-level nodes must contain the unit test root, got " + nodeNames, nodeNames.contains("test"));
    assertTrue("top-level nodes must contain the integration test root, got " + nodeNames,
               nodeNames.contains("integration-test"));

    GrailsPsiDirectoryNode testNode = findNode(nodes, "test");
    assertNotNull(testNode);
    assertEquals(NodeWeights.TESTS_FOLDER, testNode.getNodeWeight());
    assertSame("unit test root uses the platform test icon", PlatformIcons.TEST_SOURCE_FOLDER, testNode.getNodeIcon());

    GrailsPsiDirectoryNode integrationTestNode = findNode(nodes, "integration-test");
    assertNotNull(integrationTestNode);
    assertEquals(NodeWeights.TESTS_FOLDER, integrationTestNode.getNodeWeight());
    assertSame("specialised test roots use the Grails test icon", GroovyMvcIcons.Grails_test,
               integrationTestNode.getNodeIcon());
  }

  public void testRecognisesLegacyCamelCaseTestRootNames() {
    myFixture.addFileToProject("src/test/groovy/SomeSpec.groovy", "class SomeSpec {}");
    myFixture.addFileToProject("src/integrationTest/groovy/SomeIT.groovy", "class SomeIT {}");
    myFixture.addFileToProject("src/functional-test/groovy/SomeFT.groovy", "class SomeFT {}");

    Collection<AbstractTreeNode<?>> nodes =
      new Grails3NodeProvider().createNodes(testApplication(), ViewSettings.DEFAULT);

    Set<String> nodeNames = nodes.stream()
      .filter(GrailsPsiDirectoryNode.class::isInstance)
      .map(GrailsPsiDirectoryNode.class::cast)
      .map(node -> node.getValue().getName())
      .collect(Collectors.toSet());
    assertTrue("camelCase integrationTest root must be separated, got " + nodeNames, nodeNames.contains("integrationTest"));
    assertTrue("kebab-case functional-test root must be separated, got " + nodeNames,
               nodeNames.contains("functional-test"));

    GrailsPsiDirectoryNode srcNode = findNode(nodes, "src");
    assertNotNull(srcNode);
    PsiDirectory srcDir = srcNode.getValue();
    assertFalse("camelCase integrationTest root must not duplicate under src",
                srcNode.getFilter().shouldShow(srcDir.findSubdirectory("integrationTest")));
    assertFalse("kebab-case functional-test root must not duplicate under src",
                srcNode.getFilter().shouldShow(srcDir.findSubdirectory("functional-test")));
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
      return myRoot;
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
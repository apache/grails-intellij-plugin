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
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.psi.PsiDirectory;
import com.intellij.util.PlatformIcons;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsPsiDirectoryNode;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class Grails3NodeProviderTest extends GrailsNodeProviderTestSupport {

  public void testSeparatesKebabCaseTestSourceRootsFromSrcNode() {
    myFixture.addFileToProject("src/test/groovy/SomeServiceSpec.groovy", "class SomeServiceSpec {}");
    myFixture.addFileToProject("src/integration-test/groovy/SomeServiceIT.groovy", "class SomeServiceIT {}");
    myFixture.addFileToProject("src/functional-test/groovy/SomeFunctionalFT.groovy", "class SomeFunctionalFT {}");
    myFixture.addFileToProject("src/main/groovy/SomeService.groovy", "class SomeService {}");

    Collection<AbstractTreeNode<?>> nodes =
      new Grails3NodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT);

    GrailsPsiDirectoryNode srcNode = findNode(nodes, "src");
    assertNotNull("src node must be present", srcNode);
    assertEquals(NodeWeights.SRC_FOLDERS, srcNode.getNodeWeight());
    assertNotNull("src node must have a filter that hides test roots", srcNode.getFilter());

    PsiDirectory srcDir = srcNode.getValue();
    PsiDirectory mainDir = srcDir.findSubdirectory("main");
    PsiDirectory testDir = srcDir.findSubdirectory("test");
    PsiDirectory integrationTestDir = srcDir.findSubdirectory("integration-test");
    PsiDirectory functionalTestDir = srcDir.findSubdirectory("functional-test");
    assertNotNull(mainDir);
    assertNotNull(testDir);
    assertNotNull(integrationTestDir);
    assertNotNull(functionalTestDir);

    assertTrue("plain sources keep showing under src", srcNode.getFilter().shouldShow(mainDir));
    assertFalse("unit test root must not duplicate under src", srcNode.getFilter().shouldShow(testDir));
    assertFalse("integration test root must not duplicate under src", srcNode.getFilter().shouldShow(integrationTestDir));
    assertFalse("functional test root must not duplicate under src", srcNode.getFilter().shouldShow(functionalTestDir));

    assertFalse("src must not claim a file under a lifted test root, or reveal dead-ends",
                srcNode.contains(myFixture.findFileInTempDir("src/test/groovy/SomeServiceSpec.groovy")));
    assertFalse("same for the integration test root",
                srcNode.contains(myFixture.findFileInTempDir("src/integration-test/groovy/SomeServiceIT.groovy")));
    assertTrue("src must still claim its own sources",
               srcNode.contains(myFixture.findFileInTempDir("src/main/groovy/SomeService.groovy")));

    Set<String> nodeNames = nodes.stream()
      .filter(GrailsPsiDirectoryNode.class::isInstance)
      .map(GrailsPsiDirectoryNode.class::cast)
      .map(node -> node.getValue().getName())
      .collect(Collectors.toSet());
    assertTrue("top-level nodes must contain src, got " + nodeNames, nodeNames.contains("src"));
    assertTrue("top-level nodes must contain the unit test root, got " + nodeNames, nodeNames.contains("test"));
    assertTrue("top-level nodes must contain the integration test root, got " + nodeNames,
               nodeNames.contains("integration-test"));
    assertTrue("top-level nodes must contain the functional test root, got " + nodeNames,
               nodeNames.contains("functional-test"));

    GrailsPsiDirectoryNode testNode = findNode(nodes, "test");
    assertNotNull("unit test root node must be present", testNode);
    assertEquals(NodeWeights.TESTS_FOLDER, testNode.getNodeWeight());
    assertSame("unit test root uses the platform test icon", PlatformIcons.TEST_SOURCE_FOLDER, testNode.getNodeIcon());

    GrailsPsiDirectoryNode integrationTestNode = findNode(nodes, "integration-test");
    assertNotNull("integration test root node must be present", integrationTestNode);
    assertEquals(NodeWeights.TESTS_FOLDER, integrationTestNode.getNodeWeight());
    assertSame("specialised test roots use the Grails test icon", GroovyMvcIcons.Grails_test,
               integrationTestNode.getNodeIcon());

    GrailsPsiDirectoryNode functionalTestNode = findNode(nodes, "functional-test");
    assertNotNull("functional test root node must be present", functionalTestNode);
    assertEquals(NodeWeights.TESTS_FOLDER, functionalTestNode.getNodeWeight());
    assertSame("specialised test roots use the Grails test icon", GroovyMvcIcons.Grails_test,
               functionalTestNode.getNodeIcon());
  }

  public void testCamelCaseTestRootsStayUnderSrc() {
    myFixture.addFileToProject("src/main/groovy/SomeService.groovy", "class SomeService {}");
    myFixture.addFileToProject("src/integrationTest/groovy/SomeIT.groovy", "class SomeIT {}");
    myFixture.addFileToProject("src/functionalTest/groovy/SomeFT.groovy", "class SomeFT {}");

    Collection<AbstractTreeNode<?>> nodes =
      new Grails3NodeProvider().createNodes(testApplication(false), ViewSettings.DEFAULT);

    GrailsPsiDirectoryNode srcNode = findNode(nodes, "src");
    assertNotNull("src node must be present", srcNode);

    PsiDirectory srcDir = srcNode.getValue();
    PsiDirectory mainDir = srcDir.findSubdirectory("main");
    PsiDirectory integrationTestDir = srcDir.findSubdirectory("integrationTest");
    PsiDirectory functionalTestDir = srcDir.findSubdirectory("functionalTest");
    assertNotNull(mainDir);
    assertNotNull(integrationTestDir);
    assertNotNull(functionalTestDir);

    assertTrue("plain sources keep showing under src", srcNode.getFilter().shouldShow(mainDir));
    assertTrue("Grails 6 camelCase integration test root stays visible under src",
               srcNode.getFilter().shouldShow(integrationTestDir));
    assertTrue("Grails 6 camelCase functional test root stays visible under src",
               srcNode.getFilter().shouldShow(functionalTestDir));
    assertTrue("a camelCase test root is not lifted, so src still claims its files",
               srcNode.contains(myFixture.findFileInTempDir("src/integrationTest/groovy/SomeIT.groovy")));

    assertNull("camelCase integrationTest must not be lifted to a top-level node",
               findNode(nodes, "integrationTest"));
    assertNull("camelCase functionalTest must not be lifted to a top-level node",
               findNode(nodes, "functionalTest"));
  }
}

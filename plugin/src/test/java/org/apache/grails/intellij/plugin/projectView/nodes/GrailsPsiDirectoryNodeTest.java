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

package org.apache.grails.intellij.plugin.projectView.nodes;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;

import javax.swing.Icon;
import java.util.List;
import java.util.Objects;

public class GrailsPsiDirectoryNodeTest extends GrailsTestCase {

  public void testRendersCustomTitleAndIcon() {
    GrailsPsiDirectoryNode node = nodeWithCustomPresentation("grails-app/i18n/messages.properties", "Translations",
                                                           AllIcons.FileTypes.Properties, NodeWeights.CONFIG_FOLDER);

    PresentationData data = rendered(node);

    assertEquals("Translations", data.getPresentableText());
    assertSame(AllIcons.FileTypes.Properties, data.getIcon(false));
  }

  /**
   * Gradle's per-source-set modules make test directories module content roots, and only content roots
   * receive the platform's coloured fragments, which take precedence over {@code presentableText}. Every
   * other directory is labelled by {@code setPresentableText} from
   * {@code ProjectViewDirectoryHelper.getNodeName} and gets no fragments, which is why a nested directory
   * like {@code grails-app/i18n} has nothing for this hook to replace.
   *
   * <p>So the directory is registered as a content entry: that is what makes the platform write its
   * fragments first, reproducing the case where a test root failed to render as {@code Tests:unit}. The
   * assertions below cover both halves — that the platform's own fragments are present beforehand, and
   * that the title replaces them.
   */
  public void testTitleReplacesTheModuleContentRootFragments() {
    PsiDirectory directory = findDirectoryCreatedBy("src/test/ExampleSpec.groovy");
    ModuleRootModificationUtil.updateModel(getModule(), model -> model.addContentEntry(directory.getVirtualFile()));
    GrailsPsiDirectoryNode node = new GrailsPsiDirectoryNode(directory, ViewSettings.DEFAULT, null,
                                                             NodeWeights.TESTS_FOLDER, "Tests:unit", null, "src/test");

    PresentationData data = new PresentationData();
    node.updateImpl(data);

    assertFalse("a module content root must have platform fragments before postprocess",
                drawnFragments(data).isEmpty());
    assertFalse("the platform fragments must still contain the original content-root label",
                drawnFragments(data).contains("Tests:unit"));

    node.postprocess(data);

    assertEquals("the fragments the renderer draws must be the title alone",
                 List.of("Tests:unit"), drawnFragments(data));
    assertEquals("Tests:unit", data.getPresentableText());
    assertEquals("src/test", data.getLocationString());
  }

  /** An untitled node keeps the platform's own fragments, so nothing is lost by not clearing them. */
  public void testWithoutCustomPresentationKeepsPlatformFragments() {
    PsiDirectory directory = findDirectoryCreatedBy("src/test/ExampleSpec.groovy");
    ModuleRootModificationUtil.updateModel(getModule(), model -> model.addContentEntry(directory.getVirtualFile()));
    GrailsPsiDirectoryNode node = new GrailsPsiDirectoryNode(directory, ViewSettings.DEFAULT);

    PresentationData data = new PresentationData();
    node.updateImpl(data);
    List<String> platformFragments = drawnFragments(data);
    assertFalse("a module content root must have platform fragments", platformFragments.isEmpty());

    node.postprocess(data);

    assertEquals("an untitled node must preserve the platform's content-root label",
                 platformFragments, drawnFragments(data));
  }

  public void testLocationSurvivesAlongsideTheTitle() {
    PsiDirectory directory = findDirectoryCreatedBy("grails-app/views/index.gsp");
    GrailsPsiDirectoryNode node = new GrailsPsiDirectoryNode(directory, ViewSettings.DEFAULT, null,
                                                             NodeWeights.VIEWS_FOLDER, "Views", null,
                                                             "src/views");

    PresentationData data = rendered(node);

    assertEquals(List.of("Views"), drawnFragments(data));
    assertEquals("src/views", data.getLocationString());
  }

  /**
   * The label is only final after {@code postprocess}, which is the hook that runs on the renderer path.
   * Asserting straight after {@code updateImpl} checks the platform's label, not ours.
   */
  private static @NotNull PresentationData rendered(@NotNull GrailsPsiDirectoryNode node) {
    PresentationData data = new PresentationData();
    node.updateImpl(data);
    node.postprocess(data);
    return data;
  }

  /** The fragments the renderer actually draws, after the platform has finished writing the label. */
  private static @NotNull List<String> drawnFragments(@NotNull PresentationData data) {
    return data.getColoredText().stream().map(fragment -> fragment.getText()).toList();
  }

  public void testWithoutCustomPresentationUsesPlainDirectoryName() {
    PsiDirectory directory = findDirectoryCreatedBy("grails-app/views/index.gsp");
    GrailsPsiDirectoryNode node = new GrailsPsiDirectoryNode(directory, ViewSettings.DEFAULT);

    PresentationData data = new PresentationData();
    node.updateImpl(data);

    assertEquals("an ordinary nested directory uses presentableText for its qualified name",
                 "grails-app.views", data.getPresentableText());
    assertTrue("an ordinary directory has no platform fragments", drawnFragments(data).isEmpty());
    assertNotSame("a bespoke icon must not appear without customization", AllIcons.FileTypes.Properties,
                  data.getIcon(false));
  }

  private GrailsPsiDirectoryNode nodeWithCustomPresentation(@NotNull String filePath,
                                                            @NotNull String title,
                                                            @NotNull Icon icon,
                                                            int weight) {
    PsiDirectory directory = findDirectoryCreatedBy(filePath);
    return new GrailsPsiDirectoryNode(directory, ViewSettings.DEFAULT, icon, weight, title);
  }

  private @NotNull PsiDirectory findDirectoryCreatedBy(@NotNull String filePath) {
    myFixture.addFileToProject(filePath, "content");
    VirtualFile file = myFixture.findFileInTempDir(filePath);
    assertNotNull(file);
    PsiDirectory directory = PsiManager.getInstance(getProject()).findDirectory(Objects.requireNonNull(file.getParent()));
    assertNotNull(directory);
    return directory;
  }
}

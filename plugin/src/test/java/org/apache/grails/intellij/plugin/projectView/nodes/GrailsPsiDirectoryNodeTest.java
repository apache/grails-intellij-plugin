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
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiManager;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.projectView.NodeWeights;

import javax.swing.Icon;
import java.util.Objects;

public class GrailsPsiDirectoryNodeTest extends GrailsTestCase {

  public void testRendersCustomTitleAndIcon() {
    GrailsPsiDirectoryNode node = nodeWithCustomPresentation("grails-app/i18n/messages.properties", "Translations",
                                                           AllIcons.FileTypes.Properties, NodeWeights.TRANSLATIONS_FOLDER);

    PresentationData data = new PresentationData();
    node.updateImpl(data);

    assertEquals("Translations", data.getPresentableText());
    assertSame(AllIcons.FileTypes.Properties, data.getIcon(false));
  }

  public void testWithoutCustomPresentationUsesPlainDirectoryName() {
    PsiDirectory directory = findDirectoryCreatedBy("grails-app/views/index.gsp");
    GrailsPsiDirectoryNode node = new GrailsPsiDirectoryNode(directory, ViewSettings.DEFAULT);

    PresentationData data = new PresentationData();
    node.updateImpl(data);

    assertFalse("no custom title must be applied without customization",
                data.getPresentableText().contains("Translations"));
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
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
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.UserDataHolderBase;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.config.GrailsConstants;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsPsiDirectoryNode;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.util.version.Version;

import java.util.Collection;
import javax.swing.Icon;

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

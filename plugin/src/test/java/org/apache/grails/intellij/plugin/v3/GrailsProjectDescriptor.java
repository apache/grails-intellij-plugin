/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.grails.intellij.plugin.v3;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.ex.temp.TempFileSystem;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.fixtures.DefaultLightProjectDescriptor;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

// Extends the plain DefaultLightProjectDescriptor (Mock JDK 11) rather than
// LibraryLightProjectDescriptor: since 2026.2 the RepositoryTestLibrary-backed descriptor fails to
// initialize the light project ("Cannot find IntelliJ IDEA project files"), and Grails trait/artifact
// support is provided by the plugin, not a downloaded Groovy jar.
public class GrailsProjectDescriptor extends DefaultLightProjectDescriptor {
  private final String mySourceRootPath;

  public GrailsProjectDescriptor(String sourceRootPath) {
    super(IdeaTestUtil::getMockJdk11);
    mySourceRootPath = sourceRootPath;
  }

  @Override
  public VirtualFile createDirForSources(@NotNull Module module) {
    return createSourceRoot(module, mySourceRootPath);
  }

  @Override
  protected VirtualFile doCreateSourceRoot(VirtualFile root, String srcPath) {
    try {
      TempFileSystem tempFs = (TempFileSystem)root.getFileSystem();
      for (String each : StringUtil.split(srcPath, "/")) {
        VirtualFile child = root.findChild(each);
        if (child != null && tempFs.exists(child)) child.delete(this);
        root = root.createChildDirectory(this, each);
      }
    }
    catch (IOException e) {
      throw new RuntimeException(e);
    }

    return root;
  }
}

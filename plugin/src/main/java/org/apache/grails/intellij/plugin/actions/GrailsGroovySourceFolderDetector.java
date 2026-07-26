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

package org.apache.grails.intellij.plugin.actions;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.jetbrains.plugins.groovy.actions.GroovySourceFolderDetector;

final class GrailsGroovySourceFolderDetector extends GroovySourceFolderDetector {
  private static final String[] GROOVY_FOLDERS =
    {"grails-app/controllers", "grails-app/domain", "grails-app/services", "grails-app/taglib", "src/groovy"};

  @Override
  public boolean isGroovySourceFolder(PsiDirectory file) {
    Module module = ModuleUtilCore.findModuleForPsiElement(file);
    VirtualFile appRoot = GrailsFramework.getInstance().findAppRoot(module);
    if (appRoot == null) return false;

    assert module != null;
    if (GrailsFramework.getInstance().getSdkRoot(module) == null) return false;

    String path = VfsUtilCore.getRelativePath(file.getVirtualFile(), appRoot, '/');
    if (path == null) return false;

    for (String groovyFolder : GROOVY_FOLDERS) {
      if (path.equals(groovyFolder) || (path.startsWith(groovyFolder) && path.startsWith("/", groovyFolder.length()))) {
        return true;
      }
    }

    return false;
  }
}

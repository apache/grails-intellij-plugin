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

package org.apache.grails.intellij.plugin;

import com.intellij.codeInsight.daemon.ProblemHighlightFilter;
import com.intellij.lang.Language;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.apache.grails.intellij.plugin.lang.gsp.GspLanguage;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.GroovyLanguage;

public final class TemplateHighlightErrorFilter extends ProblemHighlightFilter {

  @Override
  public boolean shouldHighlight(@NotNull PsiFile psiFile) {
    Language language = psiFile.getLanguage();

    if (language == GroovyLanguage.INSTANCE || language == GspLanguage.INSTANCE) {
      VirtualFile file = psiFile.getOriginalFile().getVirtualFile();
      if (file != null) {
        ProjectFileIndex fileIndex = ProjectRootManager.getInstance(psiFile.getProject()).getFileIndex();
        if (!fileIndex.isInSource(file)) {
          VirtualFile templateDirectory = GrailsUtils.findParent(file, GrailsUtils.TEMPLATES_DIR);
          if (templateDirectory != null) {
            VirtualFile tempParent = templateDirectory.getParent();
            if (tempParent != null && tempParent.getName().equals("src")) {
              VirtualFile root = tempParent.getParent();
              if (Comparing.equal(root, fileIndex.getContentRootForFile(file))) {
                Module module = fileIndex.getModuleForFile(file);

                GrailsFramework framework = GrailsFramework.getInstance();

                if (module != null && (framework.hasSupport(module) || framework.isAuxModule(module))) {
                  return false;
                }
              }
            }
          }
        }
      }
    }

    return true;
  }
}

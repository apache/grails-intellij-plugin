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
package org.apache.grails.intellij.plugin.gson;

import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ProjectFileIndex;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.sun.jdi.ReferenceType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GsonConstants;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.jetbrains.plugins.groovy.extensions.debugger.ScriptPositionManagerHelper;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

public final class GsonPositionManagerDelegate extends ScriptPositionManagerHelper {

  private static final String SUFFIX = "_" + GsonConstants.EXTENSION;

  /** Mirrors the runtime template-class naming: every non-word character becomes an underscore. */
  private static @NotNull String clean(@NotNull String value) {
    return value.replaceAll("[\\W\\s]", "_");
  }

  @Override
  public boolean isAppropriateScriptFile(@NotNull GroovyFile scriptFile) {
    return GsonUtils.isGsonFile(scriptFile) && GrailsApplicationManager.findApplication(scriptFile) != null;
  }

  /**
   * grails.views.resolve.GenericGroovyTemplateResolver#resolveTemplateName
   */
  @Override
  public @Nullable String getRuntimeScriptName(@NotNull GroovyFile groovyFile) {
    GrailsApplication application = GrailsApplicationManager.findApplication(groovyFile);
    if (application == null) return null;
    VirtualFile file = groovyFile.getVirtualFile();
    if (file == null) return null;
    VirtualFile viewsRoot = ProjectFileIndex.getInstance(groovyFile.getProject()).getSourceRootForFile(file);
    if (viewsRoot == null) return null;
    String path = VfsUtilCore.getRelativePath(file, viewsRoot);
    if (path == null) return null;
    return clean(application.getName()) + "_" + path.replace("/", "_").replace(".", "_");
  }

  @Override
  public boolean isAppropriateRuntimeName(@NotNull String runtimeName) {
    return runtimeName.endsWith(SUFFIX);
  }

  @Override
  public @Nullable String customizeClassName(@NotNull PsiClass psiClass) {
    return psiClass.getContainingFile() instanceof GroovyFile file ? getRuntimeScriptName(file) : null;
  }

  @Override
  public @Nullable PsiFile getExtraScriptIfNotFound(@NotNull ReferenceType refType,
                                                    @NotNull String runtimeName,
                                                    @NotNull Project project,
                                                    @NotNull GlobalSearchScope scope) {
    String appNameAndViewPath = runtimeName.endsWith(SUFFIX)
                                ? runtimeName.substring(0, runtimeName.length() - SUFFIX.length())
                                : runtimeName;
    for (GrailsApplication app : GrailsApplicationManager.getInstance(project).getApplications()) {
      String appPrefix = clean(app.getName());
      String viewPath = (appNameAndViewPath.startsWith(appPrefix)
                         ? appNameAndViewPath.substring(appPrefix.length())
                         : appNameAndViewPath).replace("_", "/");
      VirtualFile view = app.getAppRoot().findFileByRelativePath("views" + viewPath + GsonConstants.FILE_SUFFIX);
      if (view == null) continue;
      if (scope.contains(view)) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(view);
        if (psiFile != null) return psiFile;
      }
    }
    return null;
  }
}

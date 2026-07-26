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
package org.jetbrains.plugins.grails.actions;

import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.actionSystem.DataKey;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.DumbService;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.structure.GrailsApplicationManager;
import org.jetbrains.plugins.grails.tests.GrailsTestUtils;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.groovy.util.GroovyUtils;

public final class GrailsActionUtil {

  private GrailsActionUtil() {
  }

  public static final DataKey<GrailsApplication> GRAILS_APPLICATION = DataKey.create("grails.application");
  public static final DataKey<GrailsArtefactHandler> GRAILS_ARTEFACT_HANDLER = DataKey.create("grails.artefact.handler");
  public static final DataKey<String> GRAILS_ARTEFACT_PACKAGE = DataKey.create("grails.artefact.package");

  public static @Nullable ArtefactData getArtefactData(@Nullable DataContext context) {
    if (context == null) return null;
    Project project = context.getData(LangDataKeys.PROJECT);
    if (project == null) return null;
    if (DumbService.isDumb(project)) return null;
    Module module = context.getData(PlatformCoreDataKeys.MODULE);
    if (module == null) return null;
    VirtualFile file = context.getData(LangDataKeys.VIRTUAL_FILE);
    if (file == null) return null;
    GrailsApplication application = GrailsApplicationManager.getInstance(project).findApplication(file);
    if (application == null) return null;
    PsiClass publicClass = GroovyUtils.getPublicClass(project, file);

    boolean isView;
    String packageName;
    String artefactName;

    if (publicClass == null) {
      // inside a view
      String controllerName = GrailsUtils.getControllerNameByGsp(file);
      if (controllerName == null) return null;
      if ("layouts".equals(controllerName) || !StringUtil.isJavaIdentifier(controllerName)) return null;
      isView = true;
      packageName = null; // we do not know package here
      artefactName = controllerName;
    }
    else {
      PsiClass artefactClass = GrailsUtils.isInGrailsTests(file, project)
                               ? GrailsTestUtils.getTestedClass(publicClass)
                               : publicClass;
      if (artefactClass == null) return null;
      String qualifiedName = artefactClass.getQualifiedName();
      if (qualifiedName == null) return null;
      isView = false;
      packageName = StringUtil.getPackageName(qualifiedName);
      GrailsArtifact type = GrailsArtifact.getType(artefactClass);
      if (type == null) return null;
      artefactName = type.getArtifactName(artefactClass);
    }

    return new ArtefactData(project, module, file, packageName, artefactName, application, isView);
  }

  public static @Nullable GrailsApplication getGrailsApplication(@NotNull DataContext dataContext) {
    GrailsApplication fromContext = dataContext.getData(GRAILS_APPLICATION);
    if (fromContext != null) return fromContext;
    Project project = CommonDataKeys.PROJECT.getData(dataContext);
    if (project == null) return null;
    VirtualFile virtualFile = CommonDataKeys.VIRTUAL_FILE.getData(dataContext);
    if (virtualFile == null) return null;
    return GrailsApplicationManager.getInstance(project).findApplication(virtualFile);
  }

  public static @Nullable GrailsArtefactHandler getArtefactHandler(@NotNull DataContext dataContext) {
    return dataContext.getData(GRAILS_ARTEFACT_HANDLER);
  }

  public static @Nullable String getArtefactPackage(@NotNull DataContext dataContext) {
    return dataContext.getData(GRAILS_ARTEFACT_PACKAGE);
  }
}

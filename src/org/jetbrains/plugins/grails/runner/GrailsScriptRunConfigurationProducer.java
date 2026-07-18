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

package org.jetbrains.plugins.grails.runner;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.actions.GrailsActionUtilKt;
import org.jetbrains.plugins.grails.config.GrailsFramework;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyNamesUtil;

import java.util.Objects;

final class GrailsScriptRunConfigurationProducer extends LazyRunConfigurationProducer<GrailsRunConfiguration> {
  @Override
  public @NotNull ConfigurationFactory getConfigurationFactory() {
    return GrailsRunConfigurationType.getInstance().getConfigurationFactories()[0];
  }

  @Override
  protected boolean setupConfigurationFromContext(@NotNull GrailsRunConfiguration configuration,
                                                  @NotNull ConfigurationContext context,
                                                  @NotNull Ref<PsiElement> sourceElement) {
    GrailsApplication application = GrailsActionUtilKt.getGrailsApplication(context.getDataContext());
    if (application == null) return false;
    if (application.getGrailsVersion().isAtLeast(Version.GRAILS_6_0)) return false;

    PsiElement element = context.getPsiLocation();
    if (!(element instanceof GroovyFile file)) return false;

    final VirtualFile virtualFile = file.getOriginalFile().getVirtualFile();
    if (virtualFile == null) return false;

    VirtualFile scriptDir = getScriptsDirectory(application);
    if (scriptDir == null || !scriptDir.equals(virtualFile.getParent())) return false;
    if (!file.isScript() || !GrailsFramework.isScriptFileName(virtualFile.getName())) return false;

    String scriptName = virtualFile.getNameWithoutExtension();
    configuration.setProgramParameters(GroovyNamesUtil.camelToSnake(scriptName));
    configuration.setGrailsApplication(application);
    configuration.setName("Grails: " + scriptName);

    sourceElement.set(file);

    return true;
  }

  @Override
  public boolean isConfigurationFromContext(@NotNull GrailsRunConfiguration configuration, @NotNull ConfigurationContext context) {
    PsiElement element = context.getPsiLocation();
    if (!(element instanceof GroovyFile)) return false;

    GrailsRunConfiguration fromContext = ((GrailsRunConfiguration)configuration.clone());
    if (setupConfigurationFromContext(fromContext, context, new Ref<>(element))) {
      return Objects.equals(configuration.getProgramParameters(), fromContext.getProgramParameters()) &&
             Comparing.equal(configuration.getGrailsApplicationNullable(), fromContext.getGrailsApplicationNullable());
    }
    return false;
  }

  public static @Nullable VirtualFile getScriptsDirectory(@NotNull GrailsApplication application) {
    return application.getGrailsVersion().isAtLeast(Version.GRAILS_3_0)
           ? VfsUtil.findRelativeFile(application.getRoot(), "src", "main", "scripts")
           : VfsUtil.findRelativeFile(application.getRoot(), "scripts");
  }
}

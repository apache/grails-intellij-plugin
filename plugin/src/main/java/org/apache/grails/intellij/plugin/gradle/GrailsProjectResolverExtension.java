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
package org.apache.grails.intellij.plugin.gradle;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.SimpleJavaParameters;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.util.ExternalSystemConstants;
import com.intellij.openapi.externalSystem.util.Order;
import com.intellij.util.PathUtil;
import org.gradle.tooling.model.idea.IdeaModule;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.lib.grails.rt.GrailsRtMarker;
import org.jetbrains.plugins.gradle.service.project.AbstractProjectResolverExtension;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.apache.grails.intellij.lib.gradle.tooling.builder.GrailsModule;
import org.apache.grails.intellij.lib.gradle.tooling.builder.GrailsModuleModelBuilderImpl;

import java.util.Collections;
import java.util.Set;

/**
 * @author Vladislav.Soroka
 */
@Order(ExternalSystemConstants.UNORDERED)
public final class GrailsProjectResolverExtension extends AbstractProjectResolverExtension {

  @Override
  public void populateModuleExtraModels(@NotNull IdeaModule gradleModule, @NotNull DataNode<ModuleData> ideModule) {
    GrailsModule grailsModule = resolverCtx.getExtraProject(gradleModule, GrailsModule.class);
    if (grailsModule != null) {
      ideModule.createChild(GrailsModuleData.KEY, new GrailsModuleData(
        GradleConstants.SYSTEM_ID,
        grailsModule.getGrailsVersion(),
        grailsModule.getGrailsPluginId(),
        grailsModule.getShellUrls()
      ));
    }

    nextResolver.populateModuleExtraModels(gradleModule, ideModule);
  }

  @Override
  public @NotNull Set<Class<?>> getExtraProjectModelClasses() {
    return Collections.singleton(GrailsModule.class);
  }

  @Override
  public @NotNull Set<Class<?>> getToolingExtensionsClasses() {
    return Set.of(
      // grails-gradle-tooling jar
      GrailsModuleModelBuilderImpl.class,
      GrailsRtMarker.class
    );
  }

  @Override
  public void enhanceRemoteProcessing(@NotNull SimpleJavaParameters parameters) throws ExecutionException {
    parameters.getClassPath().add(PathUtil.getJarPathForClass(GrailsRtMarker.class));
  }
}

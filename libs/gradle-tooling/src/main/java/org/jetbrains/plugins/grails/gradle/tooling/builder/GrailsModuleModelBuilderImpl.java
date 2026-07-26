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

package org.jetbrains.plugins.grails.gradle.tooling.builder;

import com.intellij.gradle.toolingExtension.util.GradleVersionUtil;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.tooling.Message;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vladislav.Soroka
 */
@SuppressWarnings("SSBasedInspection")
public class GrailsModuleModelBuilderImpl implements ModelBuilderService {
  @Override
  public boolean canBuild(String modelName) {
    return GrailsModule.class.getName().equals(modelName);
  }

  @Override
  public Object buildAll(String modelName, Project project) {
    Context context = Context.from(project);
    if (context == null) return null;

    GrailsVersionInfo grailsVersionInfo = context.myGrailsVersionInfo;

    // Prefer the explicit grailsVersion project property. This must be known before adding the shell
    // dependency: the shell artifact is not always version-managed by the platform/BOM (e.g. Grails 7's
    // org.apache.grails:grails-shell-cli), so adding it without a version fails to resolve and the whole
    // model build throws, leaving the module untagged as a Grails module.
    String version = (String) project.getProperties().get("grailsVersion");

    Configuration configuration = getConfiguration(project);
    DefaultExternalModuleDependency dependency = new DefaultExternalModuleDependency(
      grailsVersionInfo.gradleDependencyGroup,
      grailsVersionInfo.shellArtifactId,
      (version == null || version.isEmpty()) ? null : version);
    configuration.getDependencies().add(dependency);

    if (version == null || version.isEmpty()) {
      version = configuration.getResolvedConfiguration().getFirstLevelModuleDependencies()
        .stream()
        .filter(dep -> grailsVersionInfo.gradleDependencyGroup.equals(dep.getModuleGroup())
                       && grailsVersionInfo.shellArtifactId.equals(dep.getModuleName()))
        .findFirst()
        .map(dep -> dep.getModuleVersion())
        .orElse(null);
    }

    List<String> paths = configuration.resolve().stream().map(file -> file.getAbsolutePath()).collect(Collectors.toList());
    return (version != null && !version.isEmpty()) ? new GrailsModuleImpl(version, context.grailsPluginCoordinates, paths) : null;
  }

  private static Configuration getConfiguration(Project project) {
    if (GradleVersionUtil.isCurrentGradleNewerThan("7.0")) {
      Configuration configuration = project.getConfigurations().getByName("implementation").copy(dependency -> false);
      configuration.setCanBeResolved(true);
      return configuration;
    } else {
      return project.getConfigurations().getByName("compile").copy(dependency -> false);
    }
  }

  @Override
  public void reportErrorMessage(
    @NotNull String modelName,
    @NotNull Project project,
    @NotNull ModelBuilderContext context,
    @NotNull Exception exception
  ) {
    context.getMessageReporter().createMessage()
      .withGroup(this)
      .withKind(Message.Kind.WARNING)
      .withTitle("Grails import errors")
      .withText("Unable to build Grails project configuration")
      .withException(exception)
      .reportMessage(project);
  }

  private static class Context {
    /**
     * Array of Grails gradle plugins (see <a href="https://grails.github.io/grails-doc/latest/guide/single.html#gradlePlugins">Grails plugins for Gradle</a>).
     * If any is present, we make assumption that this is Grails project.
     *
     */
    private static final String[] GRAILS_PLUGIN_NAME_ARRAY = {
      "grails-app",
      "grails-core",
      "grails-plugin",
      "grails-web",
      "grails-gsp",
      "grails-doc",
    };


    private final @NotNull GrailsModuleModelBuilderImpl.GrailsVersionInfo myGrailsVersionInfo;
    private final @NotNull String grailsPluginCoordinates;


    private Context(@NotNull GrailsModuleModelBuilderImpl.GrailsVersionInfo version, @NotNull String plugin) {
      myGrailsVersionInfo = version;
      grailsPluginCoordinates = plugin;
    }

    private static @Nullable Context from(@NotNull Project project) {
      for (GrailsVersionInfo version : GrailsVersionInfo.values()) {
        for (String pluginName : GRAILS_PLUGIN_NAME_ARRAY) {
          String pluginCoordinates = version.getPluginCoordinates(pluginName);
          if (project.getPlugins().hasPlugin(pluginCoordinates)) {
            return new Context(version, pluginCoordinates);
          }
        }
      }
      return null;
    }
  }

  /**
   * Cordinates parts of Grails may differ from version to version. This enum stores the possible options.
   */
  private enum GrailsVersionInfo {
    GRAILS_3("org.grails", "org.grails", "grails-shell"),
    GRAILS_7("org.apache.grails.gradle", "org.apache.grails", "grails-shell-cli");

    private final String gradlePluginGroup;
    private final String gradleDependencyGroup;
    private final String shellArtifactId;

    GrailsVersionInfo(String gradlePluginGroup, String gradleDependencyGroup, String shellArtifactId) {
      this.gradlePluginGroup = gradlePluginGroup;
      this.gradleDependencyGroup = gradleDependencyGroup;
      this.shellArtifactId = shellArtifactId;
    }

    private String getPluginCoordinates(String pluginName) {
      return gradlePluginGroup + "." + pluginName;
    }
  }
}

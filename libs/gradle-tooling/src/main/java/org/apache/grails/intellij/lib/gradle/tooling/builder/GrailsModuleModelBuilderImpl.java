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

package org.apache.grails.intellij.lib.gradle.tooling.builder;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.tooling.AbstractModelBuilderService;
import org.jetbrains.plugins.gradle.tooling.Message;
import org.jetbrains.plugins.gradle.tooling.ModelBuilderContext;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Vladislav.Soroka
 */
@SuppressWarnings("SSBasedInspection")
public class GrailsModuleModelBuilderImpl extends AbstractModelBuilderService {
  /** Title of the Build tool window message reported when the Grails shell cannot be resolved. */
  public static final String SHELL_NOT_RESOLVED_TITLE = "Grails shell could not be resolved";

  /** Title of the Build tool window message reported when the command line of Grails 9+ cannot be resolved. */
  public static final String COMMAND_LINE_NOT_RESOLVED_TITLE = "Grails command line could not be resolved";

  @Override
  public boolean canBuild(String modelName) {
    return GrailsModule.class.getName().equals(modelName);
  }

  @Override
  public Object buildAll(String modelName, Project project, ModelBuilderContext context) {
    Context grailsContext = Context.from(project);
    if (grailsContext == null) return null;

    GrailsVersionInfo grailsVersionInfo = grailsContext.myGrailsVersionInfo;

    // Prefer the explicit grailsVersion project property. This must be known before adding the shell
    // dependency: the shell artifact is not always version-managed by the platform/BOM (e.g. Grails 7's
    // org.apache.grails:grails-shell-cli), so adding it without a version fails to resolve and the whole
    // model build throws, leaving the module untagged as a Grails module.
    // findProperty rather than getProperties(): the latter reads every project property, including
    // deprecated ones, which emits Gradle 9 deprecation warnings.
    Object grailsVersionProperty = project.findProperty("grailsVersion");
    String version = grailsVersionProperty == null ? null : grailsVersionProperty.toString();
    if (version == null || version.isEmpty()) {
      // grails-core shares the Grails version and, unlike the shell, is always a (BOM-managed) project dependency
      version = findResolvedVersion(project, grailsVersionInfo.gradleDependencyGroup, "grails-core");
    }
    if (version == null || version.isEmpty()) return null;

    if (grailsVersionInfo == GrailsVersionInfo.GRAILS_7 && GrailsCommandLine.isUsedBy(version)) {
      return buildGrails9(project, context, grailsContext, version);
    }

    // A detached configuration is resolvable from the start. Copying 'implementation' instead is rejected
    // by Gradle 9+ ("Calling configuration method 'copy(Spec)' is not allowed"), since that configuration
    // is declarable only, which made the whole model build fail and left Gradle 9 projects (Grails 8)
    // untagged as Grails modules. A detached configuration gets no dependency management, hence the
    // explicit version.
    DefaultExternalModuleDependency shell = new DefaultExternalModuleDependency(
      grailsVersionInfo.gradleDependencyGroup, grailsVersionInfo.shellArtifactId, version);
    Configuration configuration = project.getConfigurations().detachedConfiguration(shell);

    return new GrailsModuleImpl(version, grailsContext.grailsPluginCoordinates,
                                resolveShellUrls(project, context, configuration, shell));
  }

  /**
   * Grails 9 publishes no shell: a project's commands run in the one command line, which is the {@code all} jar of
   * {@code org.apache.grails:grails-cli} (see {@link GrailsCommandLine}). That jar holds every class the command line
   * needs, so the dependency is not transitive.
   * <p>
   * The builds of Grails 9 from before that command line (snapshots and milestones) still publish the shell of their
   * version, and their {@code grails-cli} jar does not answer {@code help --json}; the IDE then runs the commands in the
   * shell, as for Grails 8. So the shell is resolved too. A release of Grails 9 has none, so its absence alone is not
   * reported; that neither of the two can be resolved is, once. As for Grails 8, it costs the module only its Grails
   * commands: the Grails run configuration runs {@code bootRun}.
   */
  private @NotNull GrailsModuleImpl buildGrails9(Project project, ModelBuilderContext context, Context grailsContext, String version) {
    String coordinates = GrailsCommandLine.coordinates(version);
    Dependency commandLine = project.getDependencies().create(coordinates);
    if (commandLine instanceof ModuleDependency) {
      ((ModuleDependency)commandLine).setTransitive(false);
    }
    Configuration commandLineConfiguration = project.getConfigurations().detachedConfiguration(commandLine);
    commandLineConfiguration.setTransitive(false);
    Resolution commandLineUrls = resolve(commandLineConfiguration);

    DefaultExternalModuleDependency shell = new DefaultExternalModuleDependency(
      grailsContext.myGrailsVersionInfo.gradleDependencyGroup, grailsContext.myGrailsVersionInfo.shellArtifactId, version);
    Resolution shellUrls = resolve(project.getConfigurations().detachedConfiguration(shell));

    if (commandLineUrls.urls == null && shellUrls.urls == null) {
      context.getMessageReporter().createMessage()
        .withGroup(this)
        .withKind(Message.Kind.WARNING)
        .withTitle(COMMAND_LINE_NOT_RESOLVED_TITLE)
        .withText("Unable to resolve " + coordinates + " for '" + project.getName() + "', so Grails commands are not" +
                  " available for it (nor is " + GrailsCommandLine.shellCoordinates(version) + ", the Grails shell that" +
                  " builds of Grails 9 from before its command line have). The Grails run configuration still runs the" +
                  " application with the Gradle task bootRun. Add a repository that provides the Grails command line" +
                  " (Maven Central has every release), then reload the Gradle project.")
        .withException(commandLineUrls.failure)
        .reportMessage(project);
    }
    return new GrailsModuleImpl(version, grailsContext.grailsPluginCoordinates, shellUrls.urls, commandLineUrls.urls);
  }

  /** The files of a configuration, or why it could not be resolved. */
  private static final class Resolution {
    private final @Nullable List<String> urls;
    private final @Nullable RuntimeException failure;

    private Resolution(@Nullable List<String> urls, @Nullable RuntimeException failure) {
      this.urls = urls;
      this.failure = failure;
    }
  }

  private static @NotNull Resolution resolve(Configuration configuration) {
    try {
      return new Resolution(configuration.resolve().stream().map(file -> file.getAbsolutePath()).collect(Collectors.toList()), null);
    }
    catch (RuntimeException e) {
      return new Resolution(null, e);
    }
  }

  private static @Nullable String findResolvedVersion(Project project, String group, String name) {
    Configuration classpath = project.getConfigurations().findByName("compileClasspath");
    if (classpath == null) return null;
    return classpath.getResolvedConfiguration().getLenientConfiguration().getAllModuleDependencies()
      .stream()
      .filter(dep -> group.equals(dep.getModuleGroup()) && name.equals(dep.getModuleName()))
      .findFirst()
      .map(dep -> dep.getModuleVersion())
      .orElse(null);
  }

  /**
   * The shell classpath only backs the Grails command executor, so failing to resolve it (e.g. its
   * org.gradle:gradle-tooling-api dependency lives outside Maven Central) must not cost the module its
   * Grails support. The reason is reported to the Build tool window, where a user missing the Grails
   * commands looks, rather than only to the Gradle log.
   */
  private @Nullable List<String> resolveShellUrls(Project project,
                                                  ModelBuilderContext context,
                                                  Configuration configuration,
                                                  DefaultExternalModuleDependency shell) {
    try {
      return configuration.resolve().stream().map(file -> file.getAbsolutePath()).collect(Collectors.toList());
    }
    catch (RuntimeException e) {
      context.getMessageReporter().createMessage()
        .withGroup(this)
        .withKind(Message.Kind.WARNING)
        .withTitle(SHELL_NOT_RESOLVED_TITLE)
        .withText("Unable to resolve " + shell.getGroup() + ":" + shell.getName() + ":" + shell.getVersion() +
                  " for '" + project.getName() + "', so Grails commands and the Grails run configuration are not" +
                  " available for it. Add a repository that provides the shell and its dependencies (for example" +
                  " https://repo.grails.org/grails/restricted for the Gradle Tooling API), then reload the Gradle project.")
        .withException(e)
        .reportMessage(project);
      return null;
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

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

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.externalSystem.model.DataNode;
import com.intellij.openapi.externalSystem.model.ExternalProjectInfo;
import com.intellij.openapi.externalSystem.model.project.ModuleData;
import com.intellij.openapi.externalSystem.service.project.ProjectDataManager;
import com.intellij.openapi.externalSystem.util.ExternalSystemApiUtil;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.testFramework.fixtures.BuildViewTestFixture;
import junit.framework.TestCase;
import kotlin.Unit;
import org.apache.grails.intellij.lib.gradle.tooling.builder.GrailsModuleModelBuilderImpl;
import org.apache.grails.intellij.lib.testFramework.UltimateOnlyTest;
import org.apache.grails.intellij.plugin.commands.Grails9CommandLineProbe;
import org.apache.grails.intellij.plugin.commands.Grails9CommandLines;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.apache.grails.intellij.plugin.runner.GrailsCommandExecutor;
import org.apache.grails.intellij.plugin.runner.GrailsRunConfigurationType;
import org.apache.grails.intellij.plugin.runner.impl.Grails3GradleCommandExecutor;
import org.apache.grails.intellij.plugin.runner.impl.Grails9CommandExecutor;
import org.apache.grails.intellij.plugin.structure.Grails9Projects;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationProvider;
import org.jetbrains.plugins.gradle.importing.GradleImportingTestCase;
import org.jetbrains.plugins.gradle.service.project.GradleProjectResolverUtil;
import org.jetbrains.plugins.gradle.util.GradleConstants;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.junit.runners.Parameterized;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

// Apache Grails (7+) uses the org.apache.grails coordinates, and Grails 8 requires Gradle 9
// Kept off the Community run like GradleGrailsImportingTest: same Gradle test-framework base class.
@Category(UltimateOnlyTest.class)
public class GradleApacheGrailsImportingTest extends GradleImportingTestCase {
  @Parameterized.Parameter(1) public String grailsVersion;

  @SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
  @Parameterized.Parameters(name = "with Gradle-{0}, Grails-{1}")
  public static Collection<Object[]> data() {
    return Collections.singletonList(
      new Object[]{"9.8.0", "8.0.0-RC2"}
    );
  }

  /**
   * A version of Grails 9 that is never published, so that only the file repository of {@link #createGrails9Repository}
   * has its command line, and Maven Central never answers for it.
   */
  private static final String GRAILS_9_VERSION = "9.0.0-intellij-test";

  @Test
  public void importBasicGrailsProject() throws IOException {
    // repo.grails.org proxies org.gradle:gradle-tooling-api, which the Grails shell depends on
    importGrailsProject(grailsVersion, """
                          maven { url = "https://repo.grails.org/grails/restricted" }
                          """);

    assertGrailsModule(getModule("project"), grailsVersion, "org.apache.grails.gradle.grails-web", true);
    GrailsApplication application = createApplication();
    TestCase.assertTrue(ReadAction.compute(() -> GrailsRunConfigurationType.isRunnable(application)));
  }

  @Test
  public void importGrailsProjectWithUnresolvableShell() throws Exception {
    BuildViewTestFixture buildView = new BuildViewTestFixture(getMyProject());
    buildView.setUp();
    try {
      // Maven Central alone cannot resolve the Grails shell, which must not prevent Grails support
      importGrailsProject(grailsVersion, "");

      assertGrailsModule(getModule("project"), grailsVersion, "org.apache.grails.gradle.grails-web", false);
      // without the shell there is no executor for run-app, so no Grails run configuration is offered
      GrailsApplication application = createApplication();
      TestCase.assertFalse(ReadAction.compute(() -> GrailsRunConfigurationType.isRunnable(application)));
      // and the reason is a Build tool window message, not only a line in the Gradle log
      buildView.assertSyncViewTreeEquals(tree -> {
        TestCase.assertTrue(tree, tree.contains(GrailsModuleModelBuilderImpl.SHELL_NOT_RESOLVED_TITLE));
        return Unit.INSTANCE;
      });
    }
    finally {
      buildView.tearDown();
    }
  }

  /**
   * Grails 9 publishes no shell: the import resolves the command line, the {@code all} jar of
   * {@code org.apache.grails:grails-cli}, and nothing it depends on. The Grails Gradle plugin is that of the Grails 8
   * version of this test, since the import goes by {@code grailsVersion}, as the command line of Grails 9 does. That the
   * shell of the version is not there is the rule for Grails 9, and is not reported.
   */
  @Test
  public void importGrails9ProjectResolvesTheCommandLine() throws Exception {
    BuildViewTestFixture buildView = new BuildViewTestFixture(getMyProject());
    buildView.setUp();
    try {
      importGrailsProject(GRAILS_9_VERSION, createGrails9Repository(true, false));

      assertGrailsModule(getModule("project"), GRAILS_9_VERSION, "org.apache.grails.gradle.grails-web", false);
      List<String> classPath = grailsModuleData(getModule("project")).getCommandLineUrls();
      TestCase.assertEquals("the jar holds every class of the command line, so nothing it depends on is resolved: " + classPath,
                            1, classPath.size());
      TestCase.assertTrue(classPath.get(0), classPath.get(0).endsWith("grails-cli-" + GRAILS_9_VERSION + "-all.jar"));

      GrailsApplication application = createApplication();
      TestCase.assertTrue(Grails9Projects.isGrails9OrLater(application));
      TestCase.assertTrue(ReadAction.compute(() -> GrailsCommandExecutor.getGrailsExecutor(application) instanceof Grails9CommandExecutor));
      buildView.assertSyncViewTreeEquals(tree -> {
        TestCase.assertFalse(tree, tree.contains(GrailsModuleModelBuilderImpl.COMMAND_LINE_NOT_RESOLVED_TITLE));
        TestCase.assertFalse(tree, tree.contains(GrailsModuleModelBuilderImpl.SHELL_NOT_RESOLVED_TITLE));
        return Unit.INSTANCE;
      });
    }
    finally {
      buildView.tearDown();
    }
  }

  /**
   * A build of Grails 9 from before its command line publishes the Grails shell of its version too, and its jar of
   * {@code grails-cli} does not answer {@code help --json}: the import resolves both, and the commands run in the shell.
   * The stand-in jar of the command line here is no jar at all, so it answers nothing.
   */
  @Test
  public void importGrails9ProjectFallsBackToTheShellOfAnEarlierBuild() throws Exception {
    importGrailsProject(GRAILS_9_VERSION, createGrails9Repository(true, true));

    GrailsModuleData data = grailsModuleData(getModule("project"));
    TestCase.assertEquals(data.getCommandLineUrls().toString(), 1, data.getCommandLineUrls().size());
    TestCase.assertEquals(data.getShellUrls().toString(), 1, data.getShellUrls().size());
    TestCase.assertTrue(data.getShellUrls().get(0), data.getShellUrls().get(0).endsWith("grails-shell-cli-" + GRAILS_9_VERSION + ".jar"));

    GrailsApplication application = createApplication();
    Grails9CommandLineProbe.Result answer =
      Grails9CommandLines.getInstance(getMyProject()).capability(application).get(60, TimeUnit.SECONDS);
    TestCase.assertEquals(answer.detail(), Grails9CommandLineProbe.Kind.NO_DOCUMENT, answer.kind());
    TestCase.assertTrue(ReadAction.compute(() -> GrailsCommandExecutor.getGrailsExecutor(application) instanceof Grails3GradleCommandExecutor));
    TestCase.assertTrue(ReadAction.compute(() -> GrailsRunConfigurationType.isRunnable(application)));
  }

  @Test
  public void importGrails9ProjectWithUnresolvableCommandLine() throws Exception {
    BuildViewTestFixture buildView = new BuildViewTestFixture(getMyProject());
    buildView.setUp();
    try {
      importGrailsProject(GRAILS_9_VERSION, createGrails9Repository(false, false));

      assertGrailsModule(getModule("project"), GRAILS_9_VERSION, "org.apache.grails.gradle.grails-web", false);
      // the Grails run configuration runs bootRun, which needs no command line
      GrailsApplication application = createApplication();
      TestCase.assertTrue(ReadAction.compute(() -> GrailsRunConfigurationType.isRunnable(application)));
      buildView.assertSyncViewTreeEquals(tree -> {
        TestCase.assertTrue(tree, tree.contains(GrailsModuleModelBuilderImpl.COMMAND_LINE_NOT_RESOLVED_TITLE));
        return Unit.INSTANCE;
      });
    }
    finally {
      buildView.tearDown();
    }
  }

  /**
   * A Maven repository in the project with the Grails BOM of {@link #GRAILS_9_VERSION}, which the Grails Gradle plugin
   * applies, the command line of that version when {@code withCommandLine}, and the Grails shell of that version when
   * {@code withShell}, as a build of Grails 9 from before its command line has. The POM of the command line names a
   * dependency the repository does not have, so that resolving the command line with its dependencies would fail the
   * import.
   *
   * @return the declaration of the repository, for the {@code repositories} block of the build
   */
  private String createGrails9Repository(boolean withCommandLine, boolean withShell) throws IOException {
    String bom = "fixture-repo/org/apache/grails/grails-bom/" + GRAILS_9_VERSION + "/grails-bom-" + GRAILS_9_VERSION;
    createProjectSubFile(bom + ".pom", pom("grails-bom", "<packaging>pom</packaging>"));
    if (withCommandLine) {
      String cli = "fixture-repo/org/apache/grails/grails-cli/" + GRAILS_9_VERSION + "/grails-cli-" + GRAILS_9_VERSION;
      createProjectSubFile(cli + ".pom", pom("grails-cli", """
        <dependencies>
          <dependency>
            <groupId>org.apache.grails</groupId>
            <artifactId>grails-forge-cli</artifactId>
            <version>%s</version>
          </dependency>
        </dependencies>
        """.formatted(GRAILS_9_VERSION)));
      createProjectSubFile(cli + "-all.jar", "the command line");
    }
    if (withShell) {
      String shell = "fixture-repo/org/apache/grails/grails-shell-cli/" + GRAILS_9_VERSION + "/grails-shell-cli-" + GRAILS_9_VERSION;
      createProjectSubFile(shell + ".pom", pom("grails-shell-cli", ""));
      createProjectSubFile(shell + ".jar", "the shell");
    }
    return "maven { url = uri('fixture-repo') }\n";
  }

  private static String pom(String artifactId, String body) {
    return """
      <project xmlns="http://maven.apache.org/POM/4.0.0">
        <modelVersion>4.0.0</modelVersion>
        <groupId>org.apache.grails</groupId>
        <artifactId>%s</artifactId>
        <version>%s</version>
      %s</project>
      """.formatted(artifactId, GRAILS_9_VERSION, body);
  }

  private GrailsApplication createApplication() {
    VirtualFile appRoot = GrailsFramework.getInstance().findAppRoot(getModule("project"));
    TestCase.assertNotNull(appRoot);
    GrailsApplication application = ReadAction.compute(
      () -> GrailsApplicationProvider.createGrailsApplication(getModule("project").getProject(), appRoot));
    TestCase.assertNotNull(application);
    return application;
  }

  /**
   * @param projectGrailsVersion the {@code grailsVersion} of {@code gradle.properties}; the Grails Gradle plugin is that
   *                             of the {@link #grailsVersion} of the test whatever it is
   */
  private void importGrailsProject(String projectGrailsVersion, String extraRepositories) throws IOException {
    createProjectSubDirs("grails-app/conf", "grails-app/controllers", "grails-app/domain", "grails-app/i18n",
                         "grails-app/services", "grails-app/taglib", "grails-app/views/layouts", "src/main/groovy");
    createProjectSubFile("gradle.properties", "grailsVersion=" + projectGrailsVersion);

    // grails-gsp is left out on purpose: the GroovyPagePlugin (still as of 8.0.0-RC2) trips a Gradle 10
    // deprecation (a Provider used as a boolean), and these tests run Gradle with warning mode "fail" so
    // that deprecated API use in the plugin's own model builder cannot slip through.
    importProject(createBuildScriptBuilder().withBuildScriptMavenCentral().withMavenCentral()
                    .addBuildScriptPostfix("""
                                             repositories {
                                                 gradlePluginPortal()
                                             }
                                             dependencies {
                                                classpath platform("org.apache.grails:grails-bom:%s")
                                                classpath "org.apache.grails:grails-gradle-plugins"
                                             }
                                             """.formatted(grailsVersion))
                    .addPostfix("""
                                  version = "0.1"
                                  group = "myapp"
                                  apply plugin:"war"
                                  apply plugin:"org.apache.grails.gradle.grails-web"

                                  repositories {
                                  """ + extraRepositories + """
                                  }
                                  """).generate());
  }

  /**
   * The Grails model built inside the Gradle daemon is what makes the plugin treat the module as a Grails
   * application, so it must be present on the imported module (a mere grails-app folder is not enough).
   */
  static void assertGrailsModule(Module module, String grailsVersion, String grailsPluginId, boolean withShell) {
    String modulePath = ExternalSystemApiUtil.getExternalProjectPath(module);
    TestCase.assertNotNull(modulePath);
    ExternalProjectInfo projectInfo =
      ProjectDataManager.getInstance().getExternalProjectData(module.getProject(), GradleConstants.SYSTEM_ID, modulePath);
    TestCase.assertNotNull(projectInfo);
    TestCase.assertNotNull(projectInfo.getExternalProjectStructure());

    DataNode<ModuleData> moduleNode = GradleProjectResolverUtil.findModule(projectInfo.getExternalProjectStructure(), modulePath);
    TestCase.assertNotNull(moduleNode);

    DataNode<GrailsModuleData> grailsNode = ExternalSystemApiUtil.find(moduleNode, GrailsModuleData.KEY);
    TestCase.assertNotNull("Grails model is missing, see the 'Grails import errors' build message", grailsNode);
    TestCase.assertEquals(grailsVersion, grailsNode.getData().getGrailsVersion());
    TestCase.assertEquals(grailsPluginId, grailsNode.getData().getGrailsPluginId());
    TestCase.assertEquals(withShell, !grailsNode.getData().getShellUrls().isEmpty());
  }

  static GrailsModuleData grailsModuleData(Module module) {
    String modulePath = ExternalSystemApiUtil.getExternalProjectPath(module);
    ExternalProjectInfo projectInfo =
      ProjectDataManager.getInstance().getExternalProjectData(module.getProject(), GradleConstants.SYSTEM_ID, modulePath);
    DataNode<ModuleData> moduleNode = GradleProjectResolverUtil.findModule(projectInfo.getExternalProjectStructure(), modulePath);
    return ExternalSystemApiUtil.find(moduleNode, GrailsModuleData.KEY).getData();
  }
}

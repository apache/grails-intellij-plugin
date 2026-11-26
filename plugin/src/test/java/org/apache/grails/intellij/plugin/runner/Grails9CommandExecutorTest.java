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

package org.apache.grails.intellij.plugin.runner;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import org.apache.grails.intellij.lib.grails.rt.GrailsRtMarker;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.lib.testFramework.GrailsTestUtil;
import org.apache.grails.intellij.plugin.commands.Grails9HelpDocument;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;
import org.apache.grails.intellij.plugin.runner.impl.Grails3GradleCommandExecutor;
import org.apache.grails.intellij.plugin.runner.impl.Grails3InstallationCommandExecutor;
import org.apache.grails.intellij.plugin.runner.impl.Grails9CommandExecutor;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.GradleGrailsApplications;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;

/**
 * The commands of a project of Grails 9 run in the command line of Grails 9, {@code org.apache.grails:grails-cli}, as
 * upgrade note 51 of the Grails guide asks of a tool, started by the class the manifest of its jar names; those of
 * Grails 3 to 8 keep running in the Grails shell.
 */
public class Grails9CommandExecutorTest extends GrailsTestCase {

  private static final String CLI_JAR = "/repo/org/apache/grails/grails-cli/9.0.0/grails-cli-9.0.0-all.jar";
  private static final String SHELL_JAR = "/repo/org/apache/grails/grails-shell-cli/8.1.2/grails-shell-cli-8.1.2.jar";

  private Grails3Application application(String grailsVersion, List<String> classPath) {
    VirtualFile root = myFixture.addFileToProject("shop/gradle.properties", "grailsVersion=" + grailsVersion).getVirtualFile().getParent();
    return GradleGrailsApplications.application(getProject(), root, grailsVersion, classPath);
  }

  public void testAProjectOfGrails9RunsTheCommandLineOfGrails9() {
    Grails3Application grails9 = application("9.0.0", List.of(CLI_JAR));

    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails9), Grails9CommandExecutor.class);
    assertFalse("Grails 9 has no shell", new Grails3GradleCommandExecutor().isApplicable(grails9));
    assertFalse("the distribution of Grails 9 has no shell", new Grails3InstallationCommandExecutor().isApplicable(grails9));
  }

  /**
   * A project imported before the plugin resolved the command line of Grails 9, or a build of Grails 9 that publishes
   * the shell and no jar of the command line, runs its commands in the shell the import resolved.
   */
  public void testAProjectOfGrails9WithAShellAndNoCommandLineRunsTheShell() throws ExecutionException {
    VirtualFile root = myFixture.addFileToProject("shop/gradle.properties", "grailsVersion=9.0.0-SNAPSHOT").getVirtualFile().getParent();
    String shellJar = "/repo/org/apache/grails/grails-shell-cli/9.0.0-SNAPSHOT/grails-shell-cli-9.0.0-SNAPSHOT.jar";
    Grails3Application grails9 = GradleGrailsApplications.application(getProject(), root, "9.0.0-SNAPSHOT", List.of(CLI_JAR), List.of(shellJar));

    assertTrue("a jar of the command line that is not on disk is not asked",
               GrailsCommandExecutor.getGrailsExecutor(grails9) instanceof Grails3GradleCommandExecutor);
    JavaParameters params = new Grails3GradleCommandExecutor().createJavaParameters(grails9, MvcCommand.parse("create-controller Book"));
    assertEquals("org.grails.cli.GrailsCli", params.getMainClass());
    assertTrue(params.getClassPath().getPathList().toString(), params.getClassPath().getPathList().contains(new File(shellJar).getPath()));
    assertTrue("run-app is still bootRun", GrailsRunConfigurationType.isRunnable(grails9));
  }

  public void testAProjectOfGrails8KeepsTheShell() {
    Grails3Application grails8 = application("8.1.2", List.of(SHELL_JAR));

    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails8), Grails3GradleCommandExecutor.class);
    assertFalse(new Grails9CommandExecutor().isApplicable(grails8));
  }

  public void testTheCommandLineIsStartedAsTheContractSays() throws ExecutionException {
    Grails3Application grails9 = application("9.0.0", List.of(CLI_JAR));

    JavaParameters params = new Grails9CommandExecutor().createJavaParameters(grails9, MvcCommand.parse("create-controller com.example.Book"));

    assertEquals("a jar that cannot be read names no start class", "org.apache.grails.forge.cli.GrailsCli", params.getMainClass());
    assertEquals("the command line jar holds every class it needs, and no script of the IDE runs in it",
                 List.of(new File(CLI_JAR).getPath()), params.getClassPath().getPathList());
    assertFalse(params.getClassPath().getPathList().contains(PathUtil.getJarPathForClass(GrailsRtMarker.class)));
    assertEquals(List.of("--plain-output", "--non-interactive", "create-controller", "com.example.Book"),
                 params.getProgramParametersList().getList());
    assertTrue(params.getVMParametersList().getList().toString(), params.getVMParametersList().hasParameter("-XX:TieredStopAtLevel=1"));
    assertEquals(grails9.getRoot().getPath(), params.getWorkingDirectory());
  }

  public void testTheStartClassIsTheOneTheManifestOfTheJarNames() throws Exception {
    Path directory = Files.createTempDirectory("grails-cli");
    try {
      Path current = jar(directory.resolve("grails-cli-9.0.0-all.jar"), "org.apache.grails.forge.cli.GrailsCli");
      Path earlier = jar(directory.resolve("grails-cli-9.0.0-M1-all.jar"), "org.apache.grails.cli.GrailsCli");
      Path none = jar(directory.resolve("grails-cli-none-all.jar"), null);
      Path notAJar = Files.writeString(directory.resolve("grails-cli-broken-all.jar"), "not a jar");

      assertEquals("org.apache.grails.forge.cli.GrailsCli", Grails9CommandExecutor.mainClass(List.of(current.toString())));
      assertEquals("the class moves without the plugin having to", "org.apache.grails.cli.GrailsCli",
                   Grails9CommandExecutor.mainClass(List.of(earlier.toString())));
      assertEquals("the first jar that names one", "org.apache.grails.cli.GrailsCli",
                   Grails9CommandExecutor.mainClass(List.of(none.toString(), earlier.toString(), current.toString())));
      for (Path withoutOne : List.of(none, notAJar, directory.resolve("missing.jar"))) {
        assertEquals(withoutOne.toString(), "org.apache.grails.forge.cli.GrailsCli",
                     Grails9CommandExecutor.mainClass(List.of(withoutOne.toString())));
      }

      JavaParameters params = new Grails9CommandExecutor()
        .createJavaParameters(application("9.0.0", List.of(earlier.toString())), MvcCommand.parse("compile"));
      assertEquals("org.apache.grails.cli.GrailsCli", params.getMainClass());
    }
    finally {
      FileUtil.delete(directory);
    }
  }

  private static Path jar(Path file, String mainClass) throws IOException {
    Manifest manifest = new Manifest();
    manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");
    if (mainClass != null) {
      manifest.getMainAttributes().put(Attributes.Name.MAIN_CLASS, mainClass);
    }
    try (JarOutputStream out = new JarOutputStream(Files.newOutputStream(file), manifest)) {
      out.putNextEntry(new JarEntry("META-INF/grails-cli.txt"));
      out.closeEntry();
    }
    return file;
  }

  public void testVmOptionsOfTheCommandReachTheCommandLine() throws ExecutionException {
    Grails3Application grails9 = application("9.0.0", List.of(CLI_JAR));

    JavaParameters params = new Grails9CommandExecutor()
      .createJavaParameters(grails9, MvcCommand.parse("compile").setVmOptions("-Xmx512m -Dhttps.proxyHost=proxy"));

    assertTrue(params.getVMParametersList().hasParameter("-Xmx512m"));
    assertEquals("proxy", params.getVMParametersList().getPropertyValue("https.proxyHost"));
  }

  private static final List<String> OPTIONS = List.of("--plain-output", "--non-interactive");

  /** The commands of a web application, as its command line lists them: its own, and those of the plugins of the application. */
  private static Grails9HelpDocument webApplication() throws IOException {
    return Grails9HelpDocument.parse(
      Files.readString(Path.of(GrailsTestUtil.getTestRootPath("/testdata/grails9/help-json/web-app.json")), StandardCharsets.UTF_8));
  }

  /** A document with the commands of the web application and one more of the application, by a script or by a class. */
  private static Grails9HelpDocument withACommandOfTheApplication(String name, String registration) throws IOException {
    ObjectNode root = (ObjectNode)new ObjectMapper().readTree(
      Files.readString(Path.of(GrailsTestUtil.getTestRootPath("/testdata/grails9/help-json/web-app.json")), StandardCharsets.UTF_8));
    ObjectNode command = ((ArrayNode)root.get("commands")).addObject();
    command.put("name", name).put("description", "").put("project", true).put("origin", "application")
      .put("target", "application").put("startsApplication", !"script".equals(registration)).put("registration", registration);
    return Grails9HelpDocument.parse(root.toString());
  }

  private static List<String> programArguments(String commandLine, Grails9HelpDocument document) throws ExecutionException {
    return Grails9CommandExecutor.programArguments(MvcCommand.parse(commandLine), OPTIONS, () -> document);
  }

  private static List<String> typed(String... arguments) {
    List<String> result = new ArrayList<>(OPTIONS);
    result.addAll(List.of(arguments));
    return result;
  }

  /** Upgrade note 43: {@code grails prod war} is {@code grails war -Dgrails.env=production}. */
  public void testACommandOfTheCommandLineTakesTheEnvironmentAndPropertiesAfterItsName() throws Exception {
    Grails9HelpDocument document = webApplication();

    assertEquals(typed("assemble", "-Dgrails.env=production"), programArguments("prod assemble", document));
    assertEquals("by another of its names", typed("war", "-Dgrails.env=production"), programArguments("prod war", document));
    assertEquals(typed("run-app", "-Dgrails.env=development", "-Dfoo=bar", "--port", "9090"),
                 programArguments("-Dfoo=bar dev run-app --port 9090", document));
    assertEquals(typed("test-app", "-Dgrails.env=test"), programArguments("test test-app", document));
    assertEquals("a custom environment", typed("compile", "-Dgrails.env=staging"), programArguments("-Dgrails.env=staging compile", document));
    assertEquals("the environment the command names itself comes before the one in front of it",
                 typed("test-app", "-Dgrails.env=ci"), programArguments("test test-app -Dgrails.env=ci", document));
    assertEquals(typed("war", "-Dgrails.env=staging"), programArguments("prod -Dgrails.env=staging war", document));
  }

  /**
   * A command of the application or of a plugin is given everything after its name, {@code -Dgrails.env} too, and runs
   * in development. {@code run-tool} takes the environment in front of the name of the command it runs.
   */
  public void testACommandOfAPluginIsRunInTheEnvironmentByRunTool() throws Exception {
    Grails9HelpDocument document = webApplication();
    assertEquals("org.apache.grails:grails-data-hibernate5-cli:9.0.0-SNAPSHOT", document.getCommand("schema-export").origin());

    assertEquals(typed("run-tool", "-Dgrails.env=production", "schema-export", "--datasource=reports"),
                 programArguments("prod schema-export --datasource=reports", document));
    assertEquals(typed("run-tool", "-Dgrails.env=test", "schema-export"), programArguments("test schema-export", document));
    assertEquals(typed("run-tool", "-Dgrails.env=development", "schema-export"), programArguments("dev schema-export", document));
    assertEquals("a custom environment",
                 typed("run-tool", "-Dgrails.env=staging", "schema-export"), programArguments("-Dgrails.env=staging schema-export", document));
    assertEquals(typed("run-tool", "-Dgrails.env=staging", "schema-export"), programArguments("prod -Dgrails.env=staging schema-export", document));
    assertEquals("system properties are those of the build, not arguments of the command",
                 typed("run-tool", "-Dgrails.env=production", "-Dfoo=bar", "schema-export", "export"),
                 programArguments("-Dfoo=bar prod schema-export export", document));
    assertEquals(typed("run-tool", "-Dfoo=bar", "schema-export"), programArguments("-Dfoo=bar schema-export", document));
    assertEquals("what follows the name is the command's, and names no environment for the build",
                 typed("run-tool", "-Dgrails.env=production", "schema-export", "-Dgrails.env=ci"),
                 programArguments("prod schema-export -Dgrails.env=ci", document));
  }

  public void testACommandOfTheApplicationIsRunInTheEnvironmentByRunTool() throws Exception {
    Grails9HelpDocument document = withACommandOfTheApplication("dbm-update", "class");
    assertFalse(document.getCommand("dbm-update").isBuiltIn());
    assertFalse(document.getCommand("dbm-update").isScript());

    assertEquals(typed("run-tool", "-Dgrails.env=production", "dbm-update"), programArguments("prod dbm-update", document));
    assertEquals(typed("run-tool", "-Dgrails.env=staging", "dbm-update", "--contexts=live"),
                 programArguments("-Dgrails.env=staging dbm-update --contexts=live", document));
  }

  public void testRunCommandTakesTheEnvironmentInFrontOfTheNameOfTheCommandItRuns() throws Exception {
    Grails9HelpDocument document = webApplication();

    assertEquals(typed("run-tool", "-Dgrails.env=production", "dbm-update", "--contexts=live"),
                 programArguments("prod run-tool dbm-update --contexts=live", document));
    assertEquals(typed("run-tool", "-Dgrails.env=test", "dbm-status"), programArguments("test run-tool dbm-status", document));
    assertEquals("a custom environment",
                 typed("run-tool", "-Dgrails.env=staging", "dbm-update"), programArguments("-Dgrails.env=staging run-tool dbm-update", document));
    assertEquals(typed("run-tool", "-Dgrails.env=production", "-Dfoo=bar", "dbm-update"),
                 programArguments("-Dfoo=bar prod run-tool dbm-update", document));
    assertEquals("the environment run-tool is given itself comes before the one in front of it",
                 typed("run-tool", "-Dgrails.env=staging", "dbm-update"), programArguments("prod run-tool -Dgrails.env=staging dbm-update", document));
    assertEquals(typed("run-tool", "-D", "grails.env=staging", "dbm-update"), programArguments("prod run-tool -D grails.env=staging dbm-update", document));
    assertEquals("after the name of the command it runs, it is an argument of that command",
                 typed("run-tool", "-Dgrails.env=production", "dbm-update", "-Dgrails.env=staging"),
                 programArguments("prod run-tool dbm-update -Dgrails.env=staging", document));
    assertEquals(typed("run-tool", "-Dgrails.env=production", "--", "-Dgrails.env=staging"),
                 programArguments("prod run-tool -- -Dgrails.env=staging", document));
    assertEquals("run-tool is known without the commands of the project",
                 typed("run-tool", "-Dgrails.env=production", "dbm-update"), programArguments("prod run-tool dbm-update", Grails9HelpDocument.EMPTY));
  }

  /** The deprecated name of {@code run-tool} still works: it is kept as it is typed, and takes the environment in the same place. */
  public void testTheDeprecatedNameOfRunToolIsKeptAsItIsTyped() throws Exception {
    Grails9HelpDocument document = webApplication();

    assertEquals(typed("run-command", "-Dgrails.env=production", "dbm-update"), programArguments("prod run-command dbm-update", document));
    assertEquals(typed("run-command", "-Dgrails.env=staging", "dbm-update"),
                 programArguments("prod run-command -Dgrails.env=staging dbm-update", document));
    assertEquals(typed("run-command", "dbm-update"), programArguments("run-command dbm-update", document));
  }

  /** A standalone tool, which a build of Grails 9 lists with its own {@code target}, is run by {@code run-tool} in an environment too. */
  public void testAStandaloneToolIsRunInTheEnvironmentByRunTool() throws Exception {
    Grails9HelpDocument document = webApplication();
    assertTrue(document.getCommand("query-tail").isStandalone());

    assertEquals(typed("run-tool", "-Dgrails.env=production", "query-tail"), programArguments("prod query-tail", document));
    assertEquals(typed("query-tail"), programArguments("query-tail", document));
  }

  /** A script runs in the environment of the build, and reads {@code -Dgrails.env} among the arguments after its name. */
  public void testAScriptOfTheApplicationIsGivenTheEnvironmentAfterItsName() throws Exception {
    Grails9HelpDocument document = withACommandOfTheApplication("seed-data", "script");
    assertTrue(document.getCommand("seed-data").isScript());

    assertEquals(typed("seed-data", "-Dgrails.env=production", "orders"), programArguments("prod seed-data orders", document));
  }

  /**
   * The command line has no command of a name its document does not list, unless the build has come to know one since:
   * {@code run-tool} runs that one in the environment, and refuses every other name before anything runs.
   */
  public void testACommandTheDocumentDoesNotListIsNotRunInDevelopment() throws Exception {
    assertEquals(typed("run-tool", "-Dgrails.env=production", "dbm-update"), programArguments("prod dbm-update", webApplication()));
  }

  /**
   * Without the commands of the project, where a command takes the environment is not known, and after its name a
   * command of the application would run in development.
   */
  public void testAnEnvironmentIsNotGuessedAtWhenTheCommandsAreNotListed() throws Exception {
    for (String commandLine : List.of("prod dbm-update", "prod war", "-Dgrails.env=staging dbm-update", "test -Dfoo=bar dbm-update")) {
      try {
        programArguments(commandLine, Grails9HelpDocument.EMPTY);
        fail(commandLine);
      }
      catch (ExecutionException e) {
        String environment = commandLine.contains("staging") ? "staging" : commandLine.startsWith("test") ? "test" : "production";
        String name = commandLine.endsWith("war") ? "war" : "dbm-update";
        assertTrue(e.getMessage(), e.getMessage().contains("'run-tool -Dgrails.env=" + environment + " " + name + "'"));
        assertTrue(e.getMessage(), e.getMessage().contains("'" + name + " -Dgrails.env=" + environment + "'"));
      }
    }
    assertEquals("system properties alone stay where they were given before",
                 typed("dbm-update", "-Dfoo=bar"), programArguments("-Dfoo=bar dbm-update", Grails9HelpDocument.EMPTY));
  }

  public void testACommandWithNothingInFrontOfItIsRunAsItIsTypedWithoutTheCommandsOfTheProject() throws Exception {
    Supplier<Grails9HelpDocument> notAsked = () -> {
      throw new AssertionError("the commands of the project are not needed");
    };
    for (String commandLine : List.of("war", "schema-export --datasource=reports", "dbm-update -Dgrails.env=production",
                                      "gradle build --tests example.BookSpec", "help --json", "run-tool dbm-update",
                                      "prod run-tool dbm-update")) {
      List<String> arguments = Grails9CommandExecutor.programArguments(MvcCommand.parse(commandLine), OPTIONS, notAsked);
      if (!commandLine.startsWith("prod")) {
        assertEquals(typed(commandLine.split(" ")), arguments);
      }
    }
    assertEquals("without a command the command line shows its help", OPTIONS,
                 Grails9CommandExecutor.programArguments(new MvcCommand(), OPTIONS, notAsked));
  }

  public void testACommandLineTheImportDidNotResolveIsReported() {
    Grails3Application grails9 = application("9.0.0-SNAPSHOT", List.of());

    // the run configuration runs bootRun without the command line
    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails9), Grails9CommandExecutor.class);
    assertTrue(GrailsRunConfigurationType.isRunnable(grails9));
    try {
      new Grails9CommandExecutor().createJavaParameters(grails9, MvcCommand.parse("create-service Book"));
      fail("a command needs the command line");
    }
    catch (ExecutionException e) {
      assertTrue(e.getMessage(), e.getMessage().contains("org.apache.grails:grails-cli:9.0.0-SNAPSHOT:all"));
    }
  }
}

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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.PathUtil;
import org.apache.grails.intellij.lib.grails.rt.GrailsRtMarker;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;
import org.apache.grails.intellij.plugin.runner.impl.Grails3GradleCommandExecutor;
import org.apache.grails.intellij.plugin.runner.impl.Grails3InstallationCommandExecutor;
import org.apache.grails.intellij.plugin.runner.impl.Grails9CommandExecutor;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.GradleGrailsApplications;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
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

  public void testTheEnvironmentAndPropertiesInFrontOfTheCommandMoveBehindIt() {
    // upgrade note 43: grails prod war is grails war -Dgrails.env=production
    assertEquals(List.of("--plain-output", "--non-interactive", "war", "-Dgrails.env=production"),
                 Grails9CommandExecutor.programArguments(MvcCommand.parse("prod war")));
    assertEquals(List.of("--plain-output", "--non-interactive", "run-app", "-Dgrails.env=development", "-Dfoo=bar", "--port", "9090"),
                 Grails9CommandExecutor.programArguments(MvcCommand.parse("-Dfoo=bar dev run-app --port 9090")));
    assertEquals(List.of("--plain-output", "--non-interactive", "test-app", "-Dgrails.env=ci"),
                 Grails9CommandExecutor.programArguments(MvcCommand.parse("test test-app -Dgrails.env=ci")));
    assertEquals(List.of("--plain-output", "--non-interactive", "gradle", "build", "--tests", "example.BookSpec"),
                 Grails9CommandExecutor.programArguments(MvcCommand.parse("gradle build --tests example.BookSpec")));
    assertEquals("without a command the command line shows its help",
                 List.of("--plain-output", "--non-interactive"), Grails9CommandExecutor.programArguments(new MvcCommand()));
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

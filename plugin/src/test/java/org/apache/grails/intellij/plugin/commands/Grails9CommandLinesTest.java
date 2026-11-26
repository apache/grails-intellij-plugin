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

package org.apache.grails.intellij.plugin.commands;

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.execution.ExecutionException;
import com.intellij.notification.Notification;
import com.intellij.notification.NotificationType;
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.ContainerUtil;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.config.GrailsCommandLineSettings;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;
import org.apache.grails.intellij.plugin.runner.GrailsCommandExecutor;
import org.apache.grails.intellij.plugin.runner.GrailsRunConfigurationType;
import org.apache.grails.intellij.plugin.runner.impl.Grails3GradleCommandExecutor;
import org.apache.grails.intellij.plugin.runner.impl.Grails9CommandExecutor;
import org.apache.grails.intellij.plugin.runner.impl.Grails9RunApp;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.GradleGrailsApplications;
import org.jetbrains.annotations.NotNull;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Which command line runs the commands of a project of Grails 9 and later is found out by asking the jar of
 * {@code org.apache.grails:grails-cli} that the Gradle import resolved, not from the version alone: every build of
 * Grails 9 from before its command line has a jar of that name whose start class hands the command to the Grails shell.
 * The jars here are real ones, started as the plugin starts the command line ({@link FakeGrailsCommandLines}); the listing
 * of the commands of a project is replaced by a recorded document, which the tests of {@link Grails9CommandCatalog} cover.
 */
public class Grails9CommandLinesTest extends GrailsTestCase {

  private static final String SHELL_JAR = "/repo/org/apache/grails/grails-shell-cli/9.0.0-SNAPSHOT/grails-shell-cli-9.0.0-SNAPSHOT.jar";

  private Path myDirectory;
  private FakeGrailsCommandLines myJars;
  private final List<Notification> myNotices = new CopyOnWriteArrayList<>();
  private final AtomicInteger myListings = new AtomicInteger();

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myDirectory = Files.createTempDirectory("grails-cli-jars");
    myJars = new FakeGrailsCommandLines(myDirectory);
    // long enough for a JVM to start on a busy machine, short enough that a jar asked without GRAILS_PREFERRED_SHELL fails the test quickly
    Grails9CommandLineProbe.getInstance().setTimeout(20_000, getTestRootDisposable());
    getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(Notifications.TOPIC, new Notifications() {
      @Override
      public void notify(@NotNull Notification notification) {
        if ("Grails command line".equals(notification.getGroupId())) myNotices.add(notification);
      }
    });
    String recorded = Grails9HelpDocumentTest.recorded("web-app.json");
    Grails9CommandCatalog.getInstance(getProject()).setLoader(application -> {
      myListings.incrementAndGet();
      return recorded;
    }, getTestRootDisposable());
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      // here rather than with the test's disposable, which is disposed after the message bus of the project
      GrailsCommandLineSettings.getInstance(getProject()).setChoice(GrailsCommandLineSettings.Choice.AUTOMATIC);
      FileUtil.delete(myDirectory);
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  private Grails3Application application(String root, String grailsVersion, Path commandLine, List<String> shell) {
    VirtualFile directory = myFixture.addFileToProject(root + "/gradle.properties", "grailsVersion=" + grailsVersion).getVirtualFile().getParent();
    return GradleGrailsApplications.application(getProject(), directory, grailsVersion, List.of(commandLine.toString()), shell);
  }

  private Grails3Application application(Path commandLine, List<String> shell) {
    return application("shop", "9.0.0-SNAPSHOT", commandLine, shell);
  }

  private Grails9CommandLines commandLines() {
    return Grails9CommandLines.getInstance(getProject());
  }

  private Grails9CommandLineProbe.Result capability(Grails3Application application) throws Exception {
    return commandLines().capability(application).get(60, TimeUnit.SECONDS);
  }

  /** Chooses the command line until the end of the test. */
  private void choose(GrailsCommandLineSettings.Choice choice) {
    GrailsCommandLineSettings.getInstance(getProject()).setChoice(choice);
  }

  /** Completion runs off the event dispatch thread, where it may wait for the command line. */
  private static <T> T inBackground(Callable<T> callable) throws Exception {
    return ApplicationManager.getApplication().executeOnPooledThread(callable).get(60, TimeUnit.SECONDS);
  }

  private static List<String> completion(Grails3Application application) throws Exception {
    return inBackground(() -> ContainerUtil.map(GrailsCommandCompletionUtil.collectVariants(application, "", 0, ""),
                                                LookupElement::getLookupString));
  }

  private static List<String> programArguments(Grails3Application application) throws ExecutionException {
    return programArguments(application, "create-controller Book");
  }

  private static List<String> programArguments(Grails3Application application, String commandLine) throws ExecutionException {
    return new Grails9CommandExecutor().createJavaParameters(application, MvcCommand.parse(commandLine))
      .getProgramParametersList().getList();
  }

  public void testAJarThatAnswersTheFormatThePluginReadsIsTheCommandLine() throws Exception {
    Path jar = myJars.answering("grails-cli-9.0.0-SNAPSHOT-all.jar", FakeGrailsCommandLines.FORMAT_1);
    Grails3Application grails9 = application(jar, List.of(SHELL_JAR));

    Grails9CommandLineProbe.Result result = capability(grails9);

    assertEquals(Grails9CommandLineProbe.Kind.COMMAND_LINE, result.kind());
    assertEquals("asked as a tool asks the command line, with the variable that sends an earlier build to its Forge CLI",
                 List.of("--plain-output --non-interactive help --json | GRAILS_PREFERRED_SHELL=forge"), FakeGrailsCommandLines.starts(jar));
    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails9), Grails9CommandExecutor.class);
    assertFalse("the shell of a snapshot is not used when the command line answers", new Grails3GradleCommandExecutor().isApplicable(grails9));
    List<String> variants = completion(grails9);
    assertTrue(variants.toString(), variants.containsAll(List.of("create-controller", "url-mappings-report")));
    assertFalse("a command of the shell: " + variants, variants.contains("open"));
    assertEmpty(myNotices);
  }

  public void testAJarThatAnswersAnotherFormatStaysTheCommandLineWithoutCompletion() throws Exception {
    Path jar = myJars.answering("grails-cli-9.1.0-all.jar", FakeGrailsCommandLines.FORMAT_2);
    Grails3Application grails9 = application("shop", "9.1.0", jar, List.of(SHELL_JAR));

    Grails9CommandLineProbe.Result result = capability(grails9);

    assertEquals(Grails9CommandLineProbe.Kind.OTHER_FORMAT, result.kind());
    assertTrue("it is the command line of Grails 9, of a later contract",
               GrailsCommandExecutor.getGrailsExecutor(grails9) instanceof Grails9CommandExecutor);
    List<String> variants = completion(grails9);
    assertFalse(variants.toString(), variants.contains("create-controller"));
    assertFalse(variants.toString(), variants.contains("open"));
    assertEquals("a document the plugin cannot read is not asked for in the project", 0, myListings.get());
    assertEquals("the options the contract names, since the document cannot be read",
                 List.of("--plain-output", "--non-interactive", "create-controller", "Book"), programArguments(grails9));
    assertEquals(1, myNotices.size());
    assertEquals(NotificationType.WARNING, myNotices.get(0).getType());
    assertTrue(myNotices.get(0).getContent(), myNotices.get(0).getContent().contains("format 2"));
  }

  /**
   * The command line that is started for a command with an environment in front of it: where the command takes the
   * environment is read from the commands the command line lists in the project.
   */
  public void testTheEnvironmentInFrontOfACommandIsGivenWhereTheCommandOfTheProjectTakesIt() throws Exception {
    Path jar = myJars.answering("grails-cli-9.0.0-SNAPSHOT-all.jar", FakeGrailsCommandLines.FORMAT_1);
    Grails3Application grails9 = application(jar, List.of());
    assertEquals(Grails9CommandLineProbe.Kind.COMMAND_LINE, capability(grails9).kind());

    assertEquals("a command of a plugin would run in development with the environment after its name",
                 List.of("--plain-output", "--non-interactive", "run-tool", "-Dgrails.env=production", "schema-export"),
                 inBackground(() -> programArguments(grails9, "prod schema-export")));
    assertEquals(List.of("--plain-output", "--non-interactive", "war", "-Dgrails.env=production"),
                 inBackground(() -> programArguments(grails9, "prod war")));
    assertEquals(List.of("--plain-output", "--non-interactive", "run-tool", "-Dgrails.env=production", "schema-export"),
                 inBackground(() -> programArguments(grails9, "prod run-tool schema-export")));
    assertEquals("the commands of the project are listed once", 1, myListings.get());
  }

  /** A command line that does not list its commands is not given an environment where it might not be read. */
  public void testTheEnvironmentInFrontOfACommandIsRefusedWhenTheCommandsCannotBeListed() throws Exception {
    Path jar = myJars.answering("grails-cli-9.1.0-all.jar", FakeGrailsCommandLines.FORMAT_2);
    Grails3Application grails9 = application("shop", "9.1.0", jar, List.of());
    assertEquals(Grails9CommandLineProbe.Kind.OTHER_FORMAT, capability(grails9).kind());

    String refusal = inBackground(() -> {
      try {
        return "ran " + programArguments(grails9, "prod schema-export");
      }
      catch (ExecutionException e) {
        return e.getMessage();
      }
    });
    assertFalse("the command would run in development: " + refusal, refusal.startsWith("ran "));
    assertTrue(refusal, refusal.contains("run-tool -Dgrails.env=production schema-export"));
    assertEquals(List.of("--plain-output", "--non-interactive", "run-tool", "-Dgrails.env=production", "schema-export"),
                 inBackground(() -> programArguments(grails9, "run-tool -Dgrails.env=production schema-export")));
    assertEquals(List.of("--plain-output", "--non-interactive", "war", "-Dgrails.env=production"),
                 inBackground(() -> programArguments(grails9, "war -Dgrails.env=production")));
  }

  public void testAJarFromBeforeTheCommandLineGivesWayToTheShell() throws Exception {
    Path jar = myJars.beforeTheCommandLine("grails-cli-9.0.0-20261123.015441-38-all.jar");
    Grails3Application grails9 = application(jar, List.of(SHELL_JAR));

    long started = System.nanoTime();
    Grails9CommandLineProbe.Result result = capability(grails9);

    assertEquals(Grails9CommandLineProbe.Kind.NO_DOCUMENT, result.kind());
    assertTrue(result.detail(), result.detail().contains("exit code 2"));
    assertTrue("refused at once rather than waited for, as the shell would be: " + (System.nanoTime() - started) / 1_000_000 + " ms",
               TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - started) < 15);
    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails9), Grails3GradleCommandExecutor.class);
    assertFalse(new Grails9CommandExecutor().isApplicable(grails9));
    List<String> variants = completion(grails9);
    assertTrue("the commands of the shell, as for Grails 8: " + variants, variants.containsAll(List.of("open", "create-controller")));
    assertEquals(0, myListings.get());
    assertEquals(1, myNotices.size());
    assertEquals(NotificationType.INFORMATION, myNotices.get(0).getType());
    assertTrue(myNotices.get(0).getContent(), myNotices.get(0).getContent().contains("org.apache.grails:grails-shell-cli:9.0.0-SNAPSHOT"));
  }

  public void testAJarThatDoesNotAnswerInTimeGivesWayToTheShell() throws Exception {
    Grails9CommandLineProbe.getInstance().setTimeout(2_000, getTestRootDisposable());
    Path jar = myJars.hanging("grails-cli-9.0.0-M1-all.jar");
    Grails3Application grails9 = application("shop", "9.0.0-M1", jar, List.of(SHELL_JAR));

    Grails9CommandLineProbe.Result result = capability(grails9);

    assertEquals(Grails9CommandLineProbe.Kind.TIMED_OUT, result.kind());
    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails9), Grails3GradleCommandExecutor.class);
    assertEquals(1, myNotices.size());
    assertTrue(myNotices.get(0).getContent(), myNotices.get(0).getContent().contains("did not end within 2 seconds"));
  }

  public void testWithoutAShellAJarThatIsNotTheCommandLineStaysWithoutCompletion() throws Exception {
    Path jar = myJars.beforeTheCommandLine("grails-cli-9.0.0-SNAPSHOT-all.jar");
    Grails3Application grails9 = application(jar, List.of());

    assertEquals(Grails9CommandLineProbe.Kind.NO_DOCUMENT, capability(grails9).kind());

    assertTrue("nothing else can run the commands", GrailsCommandExecutor.getGrailsExecutor(grails9) instanceof Grails9CommandExecutor);
    List<String> variants = completion(grails9);
    assertFalse(variants.toString(), variants.contains("create-controller"));
    assertFalse(variants.toString(), variants.contains("open"));
    assertEquals(0, myListings.get());
    assertEquals(1, myNotices.size());
    assertEquals(NotificationType.WARNING, myNotices.get(0).getType());
  }

  public void testOneNoticeForEachJar() throws Exception {
    Path jar = myJars.beforeTheCommandLine("grails-cli-9.0.0-SNAPSHOT-all.jar");
    Grails3Application shop = application("shop", "9.0.0-SNAPSHOT", jar, List.of(SHELL_JAR));
    Grails3Application store = application("store", "9.0.0-SNAPSHOT", jar, List.of(SHELL_JAR));
    capability(shop);

    for (int i = 0; i < 3; i++) {
      GrailsCommandExecutor.getGrailsExecutor(shop);
      GrailsCommandExecutor.getGrailsExecutor(store);
      completion(shop);
      capability(store);
    }
    Grails9CommandCatalog.getInstance(getProject()).invalidate();
    completion(store);

    assertEquals("not a notice for every keystroke, command or application", 1, myNotices.size());
    assertEquals("the jar is asked once", 1, FakeGrailsCommandLines.starts(jar).size());

    Path other = myJars.beforeTheCommandLine("grails-cli-9.0.0-20261124.010000-39-all.jar");
    capability(application("market", "9.0.0-SNAPSHOT", other, List.of(SHELL_JAR)));
    assertEquals("another jar has a notice of its own", 2, myNotices.size());
  }

  public void testAJarIsAskedOnceUntilItChanges() throws Exception {
    Path jar = myJars.answering("grails-cli-9.0.0-SNAPSHOT-all.jar", FakeGrailsCommandLines.FORMAT_1);
    capability(application("shop", "9.0.0-SNAPSHOT", jar, List.of()));
    capability(application("store", "9.0.0-SNAPSHOT", jar, List.of()));
    assertEquals(1, FakeGrailsCommandLines.starts(jar).size());

    // a snapshot replaced in place, as in a local Maven repository
    myJars.answering("grails-cli-9.0.0-SNAPSHOT-all.jar", FakeGrailsCommandLines.FORMAT_1 + "\n");
    capability(application("market", "9.0.0-SNAPSHOT", jar, List.of()));
    assertEquals(2, FakeGrailsCommandLines.starts(jar).size());
  }

  public void testTheEventDispatchThreadDoesNotWaitForTheJar() throws Exception {
    Path jar = myJars.answeringAfter("grails-cli-9.0.0-SNAPSHOT-all.jar", "Usage: grails [OPTIONS]", 3_000);
    Grails3Application grails9 = application(jar, List.of(SHELL_JAR));

    assertTrue(ApplicationManager.getApplication().isDispatchThread());
    long started = System.nanoTime();
    assertTrue("while the jar is asked, the command line of a release of Grails 9 is taken",
               GrailsCommandExecutor.getGrailsExecutor(grails9) instanceof Grails9CommandExecutor);
    assertTrue(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started) < 2_000);

    assertEquals(Grails9CommandLineProbe.Kind.NO_DOCUMENT, capability(grails9).kind());
    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails9), Grails3GradleCommandExecutor.class);
  }

  public void testTheGlobalOptionsAreThoseTheDocumentLists() throws Exception {
    Grails3Application both = application("shop", "9.0.0", myJars.answering("both.jar", FakeGrailsCommandLines.FORMAT_1), List.of());
    Grails3Application plainOnly = application("store", "9.0.0", myJars.answering("plain-only.jar", """
      {"format":1,"complete":true,"diagnostics":[],"projectDirectory":null,"options":[{"names":["--plain-output"]}],"commands":[]}
      """), List.of());
    Grails3Application unlisted = application("market", "9.0.0", myJars.answering("no-options.jar", """
      {"format":1,"complete":true,"diagnostics":[],"projectDirectory":null,"commands":[]}
      """), List.of());

    assertEquals("before the jar has answered, the options the contract names",
                 List.of("--plain-output", "--non-interactive", "create-controller", "Book"), programArguments(plainOnly));
    capability(both);
    capability(plainOnly);
    capability(unlisted);

    assertEquals(List.of("--plain-output", "--non-interactive", "create-controller", "Book"), programArguments(both));
    assertEquals("an option the command line does not list is left out",
                 List.of("--plain-output", "create-controller", "Book"), programArguments(plainOnly));
    assertEquals("a document without options of its own says nothing against the contract",
                 List.of("--plain-output", "--non-interactive", "create-controller", "Book"), programArguments(unlisted));
  }

  public void testTheShellCanBeChosen() throws Exception {
    choose(GrailsCommandLineSettings.Choice.SHELL);
    Path jar = myJars.answering("grails-cli-9.0.0-SNAPSHOT-all.jar", FakeGrailsCommandLines.FORMAT_1);
    Grails3Application grails9 = application(jar, List.of(SHELL_JAR));

    assertNull("the jar is not asked", capability(grails9));
    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails9), Grails3GradleCommandExecutor.class);
    assertTrue(completion(grails9).contains("open"));
    assertEmpty(FakeGrailsCommandLines.starts(jar));

    choose(GrailsCommandLineSettings.Choice.AUTOMATIC);
    capability(grails9);
    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails9), Grails9CommandExecutor.class);
    assertTrue("the commands are listed again for the other command line", completion(grails9).contains("url-mappings-report"));
  }

  public void testTheCommandLineCanBeChosen() throws Exception {
    choose(GrailsCommandLineSettings.Choice.COMMAND_LINE);
    Path jar = myJars.beforeTheCommandLine("grails-cli-9.0.0-SNAPSHOT-all.jar");
    Grails3Application grails9 = application(jar, List.of(SHELL_JAR));

    assertEquals(Grails9CommandLineProbe.Kind.NO_DOCUMENT, capability(grails9).kind());
    assertInstanceOf(GrailsCommandExecutor.getGrailsExecutor(grails9), Grails9CommandExecutor.class);
    assertFalse(new Grails3GradleCommandExecutor().isApplicable(grails9));
    assertFalse(completion(grails9).contains("open"));
    assertEquals("a jar that does not answer help --json is not asked again in the project", 0, myListings.get());
    assertEquals(1, myNotices.size());
    assertTrue(myNotices.get(0).getContent(), myNotices.get(0).getContent().contains("chosen for this project"));
  }

  public void testTheChoiceIsForProjectsOfGrails9AndLaterOnly() {
    Grails3Application grails8 = GradleGrailsApplications.application(
      getProject(), myFixture.addFileToProject("old/gradle.properties", "grailsVersion=8.1.2").getVirtualFile().getParent(),
      "8.1.2", List.of("/repo/org/apache/grails/grails-shell-cli/8.1.2/grails-shell-cli-8.1.2.jar"));

    for (GrailsCommandLineSettings.Choice choice : GrailsCommandLineSettings.Choice.values()) {
      choose(choice);
      assertTrue(choice.name(), GrailsCommandExecutor.getGrailsExecutor(grails8) instanceof Grails3GradleCommandExecutor);
    }
  }

  public void testRunAppRunsWithGradleWhateverRunsTheCommands() throws Exception {
    choose(GrailsCommandLineSettings.Choice.SHELL);
    Grails3Application withoutShell = application(myJars.answering("grails-cli-9.0.0-all.jar", FakeGrailsCommandLines.FORMAT_1), List.of());

    assertNull("no shell to run the commands in", GrailsCommandExecutor.getGrailsExecutor(withoutShell));
    assertTrue("bootRun needs neither", GrailsRunConfigurationType.isRunnable(withoutShell));
    assertTrue(Grails9RunApp.runsAsBootRun(withoutShell, MvcCommand.parse("run-app --port 9090")));
    assertFalse(Grails9RunApp.runsAsBootRun(withoutShell, MvcCommand.parse("create-controller Book")));
  }
}

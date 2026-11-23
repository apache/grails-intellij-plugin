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
import com.intellij.notification.Notifications;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess;
import com.intellij.util.containers.ContainerUtil;
import org.apache.grails.intellij.lib.gradle.tooling.builder.GrailsCommandLine;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.apache.grails.intellij.plugin.config.GrailsCommandLineSettings;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.GradleGrailsApplications;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationListener;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The commands of a project of Grails 9 are those {@code grails help --json} lists in it. The document is asked for
 * once and kept until it can be out of date, and the event dispatch thread never waits for it. The listing is replaced
 * by a {@link Grails9CommandCatalog.Loader} that answers a recorded document and counts how often it is asked; the jar of
 * the command line is a real one that answers as the command line of Grails 9 does ({@link FakeGrailsCommandLines}), since
 * only such a jar is asked for the commands of a project ({@link Grails9CommandLinesTest}). The project is a real
 * directory, since what makes a document out of date is its files.
 */
public class Grails9CommandCatalogTest extends GrailsTestCase {

  private Path myRootDirectory;
  private Path myJarDirectory;
  private FakeGrailsCommandLines myJars;
  private VirtualFile myRoot;
  private final List<Notification> myNotices = new CopyOnWriteArrayList<>();
  private final AtomicInteger myListings = new AtomicInteger();
  private volatile Grails9CommandCatalog.Loader myCommandLine;

  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myRootDirectory = Files.createTempDirectory("grails9-shop");
    myJarDirectory = Files.createTempDirectory("grails9-cli");
    myJars = new FakeGrailsCommandLines(myJarDirectory);
    getProject().getMessageBus().connect(getTestRootDisposable()).subscribe(Notifications.TOPIC, new Notifications() {
      @Override
      public void notify(@NotNull Notification notification) {
        if ("Grails command line".equals(notification.getGroupId())) myNotices.add(notification);
      }
    });
    VfsRootAccess.allowRootAccess(getTestRootDisposable(), myRootDirectory.toString());
    myRoot = Objects.requireNonNull(LocalFileSystem.getInstance().refreshAndFindFileByNioFile(myRootDirectory));
    String recorded = Grails9HelpDocumentTest.recorded("web-app.json");
    myCommandLine = application -> recorded;
    catalog().setLoader(application -> {
      myListings.incrementAndGet();
      return myCommandLine.helpJson(application);
    }, getTestRootDisposable());
  }

  @Override
  protected void tearDown() throws Exception {
    try {
      FileUtil.delete(myRootDirectory);
      FileUtil.delete(myJarDirectory);
    }
    catch (Throwable e) {
      addSuppressedException(e);
    }
    finally {
      super.tearDown();
    }
  }

  private Grails9CommandCatalog catalog() {
    return Grails9CommandCatalog.getInstance(getProject());
  }

  /** An application of {@code grailsVersion}; from Grails 9 on, with a jar of the command line that answers as it does. */
  private Grails3Application application(String grailsVersion) throws IOException {
    Path jar = myJarDirectory.resolve("grails-cli-" + grailsVersion + "-all.jar");
    if (GrailsCommandLine.isUsedBy(grailsVersion) && !Files.exists(jar)) {
      myJars.answering(jar.getFileName().toString(), FakeGrailsCommandLines.FORMAT_1);
    }
    return GradleGrailsApplications.application(getProject(), myRoot, grailsVersion, List.of(jar.toString()));
  }

  /** Completion and the other callers that may wait for the command line run off the event dispatch thread. */
  private static <T> T inBackground(Callable<T> callable) throws Exception {
    return ApplicationManager.getApplication().executeOnPooledThread(callable).get(30, TimeUnit.SECONDS);
  }

  private Grails9HelpDocument document(Grails3Application application) throws Exception {
    return inBackground(() -> catalog().getDocument(application));
  }

  private void touch(String relativePath, String text) throws Exception {
    Path file = myRootDirectory.resolve(relativePath);
    Files.createDirectories(file.getParent());
    Files.writeString(file, text);
    // a later modification time than any the file had, whatever the resolution of the clock of the file system
    Files.setLastModifiedTime(file, FileTime.from(Instant.now().plusSeconds(60L + myListings.get())));
  }

  public void testCompletionOffersTheCommandsTheCommandLineLists() throws Exception {
    Grails3Application grails9 = application("9.0.0");

    List<String> variants = inBackground(() -> ContainerUtil.map(
      GrailsCommandCompletionUtil.collectVariants(grails9, "", 0, ""), LookupElement::getLookupString));

    assertTrue(variants.toString(), variants.containsAll(List.of("create-controller", "run-app", "war", "generate-all", "url-mappings-report")));
    assertFalse("a command of the Grails shell that Grails 9 does not have: " + variants, variants.contains("open"));
  }

  public void testAProjectOfGrails8KeepsTheCommandsOfTheShell() throws IOException {
    Grails3Application grails8 = application("8.1.2");

    List<String> variants = ContainerUtil.map(GrailsCommandCompletionUtil.collectVariants(grails8, "", 0, ""), LookupElement::getLookupString);

    assertTrue(variants.toString(), variants.containsAll(List.of("open", "create-controller", "run-app")));
    assertSame(Grails9HelpDocument.EMPTY, catalog().getDocument(grails8));
    assertEquals("the command line of Grails 9 is not asked about a project of Grails 8", 0, myListings.get());
  }

  public void testTheCommandLineIsAskedOnceWhileNothingChanges() throws Exception {
    Grails3Application grails9 = application("9.0.0");

    for (int i = 0; i < 3; i++) {
      assertTrue(document(grails9).getCommandNames().contains("create-controller"));
    }
    assertEquals(1, myListings.get());
  }

  public void testItIsAskedAgainWhenTheFilesTheAnswerDependsOnChange() throws Exception {
    Grails3Application grails9 = application("9.0.0");
    document(grails9);

    touch(".grails/cache/command-catalog.json", "{}");
    document(grails9);
    assertEquals("after grailsCommandCatalog listed the commands of the application again", 2, myListings.get());

    touch("src/main/scripts/load.groovy", "");
    document(grails9);
    assertEquals("after a script was added", 3, myListings.get());

    touch(".grails/project.yml", "format: 1\n");
    document(grails9);
    assertEquals("after the record of the project changed", 4, myListings.get());

    document(grails9);
    assertEquals(4, myListings.get());
  }

  public void testItIsAskedAgainForAnotherCommandLine() throws Exception {
    document(application("9.0.0"));
    document(application("9.0.1"));
    assertEquals("after the Gradle import resolved the command line of another version", 2, myListings.get());
  }

  public void testItIsAskedAgainAfterTheProjectIsReloaded() throws Exception {
    Grails3Application grails9 = application("9.0.0");
    document(grails9);

    getProject().getMessageBus().syncPublisher(ProjectDataImportListener.TOPIC).onImportFinished(myRoot.getPath());
    document(grails9);
    assertEquals(2, myListings.get());

    getProject().getMessageBus().syncPublisher(GrailsApplicationListener.TOPIC).applicationsRecomputed();
    document(grails9);
    assertEquals(3, myListings.get());
  }

  public void testAFailedListingIsKeptUntilSomethingChanges() throws Exception {
    myCommandLine = application -> {
      throw new ExecutionException("grails help --json ended with exit code 1");
    };
    Grails3Application grails9 = application("9.0.0");

    assertSame(Grails9HelpDocument.EMPTY, document(grails9));
    assertSame(Grails9HelpDocument.EMPTY, document(grails9));
    assertEquals("a broken command line is not started on every keystroke", 1, myListings.get());

    touch(".grails/cache/command-catalog.json", "{}");
    assertSame(Grails9HelpDocument.EMPTY, document(grails9));
    assertEquals(2, myListings.get());
    assertEquals("the plugin says once that the commands are not completed, not after every listing", 1, myNotices.size());
    assertTrue(myNotices.get(0).getContent(), myNotices.get(0).getContent().contains("exit code 1"));
  }

  public void testADocumentOfAnotherFormatInTheProjectIsNotRead() throws Exception {
    myCommandLine = application -> FakeGrailsCommandLines.FORMAT_2;
    Grails3Application grails9 = application("9.0.0");

    assertSame(Grails9HelpDocument.EMPTY, document(grails9));
    assertEquals(1, myNotices.size());
    assertTrue(myNotices.get(0).getContent(), myNotices.get(0).getContent().contains("format 2"));
  }

  public void testAnotherChoiceOfTheCommandLineAsksAgain() throws Exception {
    Grails3Application grails9 = application("9.0.0");
    document(grails9);

    GrailsCommandLineSettings settings = GrailsCommandLineSettings.getInstance(getProject());
    settings.setChoice(GrailsCommandLineSettings.Choice.SHELL);
    try {
      assertSame("the commands of the shell are not those of the command line", Grails9HelpDocument.EMPTY, document(grails9));
    }
    finally {
      settings.setChoice(GrailsCommandLineSettings.Choice.AUTOMATIC);
    }
    assertTrue(document(grails9).getCommandNames().contains("create-controller"));
    assertEquals(2, myListings.get());
  }

  public void testTheEventDispatchThreadDoesNotWait() throws Exception {
    Grails3Application grails9 = application("9.0.0");
    Grails9HelpDocument first = document(grails9);

    CountDownLatch release = new CountDownLatch(1);
    String recorded = Grails9HelpDocumentTest.recorded("web-app-commands-not-listed.json");
    myCommandLine = application -> {
      try {
        release.await(30, TimeUnit.SECONDS);
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return recorded;
    };
    touch(".grails/cache/command-catalog.json", "{}");

    assertTrue(ApplicationManager.getApplication().isDispatchThread());
    assertSame("while the command line runs, the document known so far is answered", first, catalog().getDocument(grails9));

    release.countDown();
    Grails9HelpDocument second = document(grails9);
    assertFalse(second.isComplete());
    assertSame(second, catalog().getDocument(grails9));
    assertEquals(2, myListings.get());
  }

  public void testPrefetchAsksInTheBackground() throws Exception {
    Grails3Application grails9 = application("9.0.0");

    catalog().prefetch(grails9);
    document(grails9);
    assertEquals(1, myListings.get());

    catalog().prefetch(application("8.1.2"));
    assertEquals(1, myListings.get());
  }
}

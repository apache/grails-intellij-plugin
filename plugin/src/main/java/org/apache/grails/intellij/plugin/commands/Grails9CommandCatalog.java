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

import com.intellij.execution.ExecutionException;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.process.ProcessOutput;
import com.intellij.execution.util.ExecUtil;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.externalSystem.service.project.manage.ProjectDataImportListener;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.concurrency.AppExecutorUtil;
import com.intellij.util.messages.MessageBusConnection;
import org.apache.grails.intellij.plugin.config.GrailsCommandLineSettings;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;
import org.apache.grails.intellij.plugin.runner.GrailsCommandExecutor;
import org.apache.grails.intellij.plugin.runner.impl.Grails9CommandExecutor;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.Grails9Projects;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationListener;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * The commands of the projects of Grails 9 and later, as {@code grails help --json} lists them in each project.
 * <p>
 * The command line of Grails 9 decides which commands a project has: those of the command line itself, the code
 * generation commands its record allows, the commands of the application and its plugins that the build listed in
 * {@code .grails/cache/command-catalog.json}, and the scripts in {@code src/main/scripts}. Rather than carry a list
 * of its own, the plugin asks the command line (upgrade note 51 of the Grails guide).
 * <p>
 * Asking starts a JVM, so a document is kept for each project and asked again only when it can be out of date: when
 * one of the files the answer depends on ({@link #INPUTS}) has changed, when the class path or version of the command
 * line has, when another command line is chosen, and after the Gradle project is reloaded or the Grails applications
 * are recomputed. A document that could not be had is kept as an empty one until then too, so a broken command line is
 * not started on every keystroke, and the plugin says so once ({@link Grails9CommandLines}). Nothing waits for the
 * command line on the event dispatch thread: there the document known so far is answered while a newer one is asked
 * for in the background.
 * <p>
 * Only a command line whose jar answered {@code help --json} with a document the plugin reads is asked
 * ({@link Grails9CommandLineProbe}): one that did not, as a build of Grails 9 from before its command line, would do no
 * better in the project, and its commands run in the Grails shell when it has one.
 */
@Service(Service.Level.PROJECT)
public final class Grails9CommandCatalog implements Disposable {

  private static final Logger LOG = Logger.getInstance(Grails9CommandCatalog.class);

  private static final int TIMEOUT_MILLIS = 60_000;

  /** The files of a project, relative to its root, that the answer of {@code help --json} depends on. */
  static final List<String> INPUTS = List.of(
    Grails9Projects.RECORD_DIRECTORY + "/" + Grails9Projects.RECORD_FILE,
    Grails9Projects.RECORD_DIRECTORY + "/forge.lock.yml",
    Grails9Projects.RECORD_DIRECTORY + "/cache/command-catalog.json",
    "grails-forge-cli.yml",
    "grails-cli.yml",
    "gradle.properties",
    "src/main/scripts",
    "grails-app/commands"
  );

  /** Asks the command line of an application for its document. */
  @FunctionalInterface
  public interface Loader {
    /** @return the standard output of {@code help --json} in the project of {@code application} */
    @NotNull String helpJson(@NotNull GrailsApplication application) throws ExecutionException;
  }

  private record Entry(@NotNull List<Object> inputs,
                       @NotNull CompletableFuture<Grails9HelpDocument> document,
                       @NotNull Grails9HelpDocument previous) {
  }

  private final Map<String, Entry> myEntries = new ConcurrentHashMap<>();
  private volatile @NotNull Loader myLoader = Grails9CommandCatalog::runHelpJson;

  public Grails9CommandCatalog(@NotNull Project project) {
    MessageBusConnection connection = project.getMessageBus().connect(this);
    connection.subscribe(GrailsApplicationListener.TOPIC, (GrailsApplicationListener)this::invalidate);
    connection.subscribe(ProjectDataImportListener.TOPIC, new ProjectDataImportListener() {
      @Override
      public void onImportFinished(String projectPath) {
        invalidate();
      }
    });
    connection.subscribe(GrailsCommandLineSettings.Listener.TOPIC, (GrailsCommandLineSettings.Listener)this::invalidate);
  }

  public static @NotNull Grails9CommandCatalog getInstance(@NotNull Project project) {
    return project.getService(Grails9CommandCatalog.class);
  }

  @Override
  public void dispose() {
    invalidate();
  }

  /** Forgets every document, so that each is asked for again when it is next needed. */
  public void invalidate() {
    myEntries.clear();
  }

  /** Asks for the document of {@code application} in the background, unless the one known is up to date. */
  public void prefetch(@NotNull GrailsApplication application) {
    if (Grails9Projects.isGrails9OrLater(application)) {
      request(application);
    }
  }

  /**
   * @return the commands of the project of {@code application}, or the empty document for a project of an earlier
   * version. Off the event dispatch thread this waits, cancellably, for a document that is being asked for; on it,
   * the one known so far is answered.
   */
  public @NotNull Grails9HelpDocument getDocument(@NotNull GrailsApplication application) {
    if (!Grails9Projects.isGrails9OrLater(application)) return Grails9HelpDocument.EMPTY;
    Entry entry = request(application);
    if (entry.document().isDone() || ApplicationManager.getApplication().isDispatchThread()) {
      return entry.document().getNow(entry.previous());
    }
    return await(entry.document());
  }

  private @NotNull Entry request(@NotNull GrailsApplication application) {
    List<Object> inputs = inputs(application);
    return myEntries.compute(application.getRoot().getPath(), (root, known) -> {
      if (known != null && known.inputs().equals(inputs)) return known;
      Grails9HelpDocument previous = known == null ? Grails9HelpDocument.EMPTY : known.document().getNow(known.previous());
      return new Entry(inputs, load(application), previous);
    });
  }

  private @NotNull CompletableFuture<Grails9HelpDocument> load(@NotNull GrailsApplication application) {
    Loader loader = myLoader;
    Grails9CommandLines commandLines = Grails9CommandLines.getInstance(application.getProject());
    return commandLines.capability(application).thenApplyAsync(answer -> {
      if (commandLines.runnerOf(application) != Grails9CommandLines.Runner.COMMAND_LINE
          || answer == null || answer.kind() != Grails9CommandLineProbe.Kind.COMMAND_LINE) {
        return Grails9HelpDocument.EMPTY;
      }
      try {
        return Grails9HelpDocument.parse(loader.helpJson(application));
      }
      catch (ExecutionException | IOException | RuntimeException e) {
        LOG.warn("The commands of the Grails project in " + application.getRoot().getPath() + " could not be listed", e);
        commandLines.listingFailed(application, StringUtil.notNullize(e.getMessage(), e.getClass().getSimpleName()));
        return Grails9HelpDocument.EMPTY;
      }
    }, AppExecutorUtil.getAppExecutorService());
  }

  private static @NotNull Grails9HelpDocument await(@NotNull CompletableFuture<Grails9HelpDocument> document) {
    while (true) {
      ProgressManager.checkCanceled();
      try {
        return document.get(50, TimeUnit.MILLISECONDS);
      }
      catch (TimeoutException ignored) {
        // ask again, after a check for cancellation
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new CancellationException("Interrupted while the commands of a Grails project were listed");
      }
      catch (java.util.concurrent.ExecutionException e) {
        return Grails9HelpDocument.EMPTY;
      }
    }
  }

  /** What the document of {@code application} depends on: the command line that answers, and the files of {@link #INPUTS}. */
  private static @NotNull List<Object> inputs(@NotNull GrailsApplication application) {
    List<Object> result = new ArrayList<>();
    if (application instanceof Grails3Application grails3) {
      result.add(grails3.getGradleData().getGrailsVersion());
      result.add(grails3.getGradleData().getCommandLineUrls());
      result.add(grails3.getGradleData().getShellUrls());
    }
    result.add(GrailsCommandLineSettings.getInstance(application.getProject()).getChoice());
    String root = application.getRoot().getPath();
    for (String input : INPUTS) {
      File file = new File(root, input);
      result.add(List.of(input, file.exists(), file.lastModified(), file.length()));
    }
    return result;
  }

  private static @NotNull String runHelpJson(@NotNull GrailsApplication application) throws ExecutionException {
    if (!(GrailsCommandExecutor.getGrailsExecutor(application) instanceof Grails9CommandExecutor executor)) {
      throw new ExecutionException("No Grails 9 command line for " + application.getRoot().getPath());
    }
    GeneralCommandLine commandLine = executor.createCommandLine(application, new MvcCommand("help", "--json"));
    ProcessOutput output = ExecUtil.execAndGetOutput(commandLine, TIMEOUT_MILLIS);
    if (output.isTimeout()) {
      throw new ExecutionException("grails help --json did not end within " + TIMEOUT_MILLIS / 1000 + " seconds");
    }
    if (output.getExitCode() != 0) {
      throw new ExecutionException("grails help --json ended with exit code " + output.getExitCode() + ": "
                                   + StringUtil.notNullize(StringUtil.substringBefore(output.getStderr().strip() + "\n", "\n")));
    }
    return output.getStdout();
  }

  /** Answers every document from {@code loader} until {@code parentDisposable} is disposed. */
  @TestOnly
  public void setLoader(@NotNull Loader loader, @NotNull Disposable parentDisposable) {
    Loader original = myLoader;
    myLoader = loader;
    invalidate();
    Disposer.register(parentDisposable, () -> {
      myLoader = original;
      invalidate();
    });
  }
}

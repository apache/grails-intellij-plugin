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

import com.intellij.notification.Notification;
import com.intellij.notification.NotificationAction;
import com.intellij.notification.NotificationType;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.options.ShowSettingsUtil;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import org.apache.grails.intellij.lib.gradle.tooling.builder.GrailsCommandLine;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.config.GrailsCommandLineSettings;
import org.apache.grails.intellij.plugin.config.GrailsCustomFoldersConfigurable;
import org.apache.grails.intellij.plugin.runner.impl.Grails9CommandExecutor;
import org.apache.grails.intellij.plugin.structure.Grails3Application;
import org.apache.grails.intellij.plugin.structure.Grails9Projects;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationListener;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Which command line runs the commands of a project of Grails 9 and later: the command line of Grails 9 or the Grails
 * shell of its version, and the notice the plugin gives when that is not what a project of Grails 9 is expected to have.
 * <p>
 * With {@link GrailsCommandLineSettings.Choice#AUTOMATIC}, the default, it is the command line of Grails 9 when its jar
 * answers {@code help --json} as that command line does ({@link Grails9CommandLineProbe}). A jar that does not, as the
 * jar of a build of Grails 9 from before its command line, gives way to the Grails shell of the version, when the
 * Gradle import resolved one, as for Grails 8. Without a shell the command line stays, without completion of the names
 * of its commands. The other two choices take one or the other whatever the jar answers.
 * <p>
 * The choice never waits for the jar on the event dispatch thread: while it is being asked, the command line of Grails 9
 * is taken, which is what a release of Grails 9 has. The jar is asked as soon as the Grails applications of the project
 * are known ({@link Prefetch}), which is long before a command is typed. Whatever the jar answers, and whatever the
 * listing of the commands of a project then comes to, the plugin says so at most once for each project and jar.
 * <p>
 * Running the application and its tests does not depend on any of this: for every project of Grails 9 and later they
 * are the Gradle tasks {@code bootRun} and {@code test}.
 */
@Service(Service.Level.PROJECT)
public final class Grails9CommandLines {

  /** What runs the commands of an application. */
  public enum Runner {
    /** The Grails shell: every application up to Grails 8, and one of Grails 9 that is chosen or found to have it. */
    SHELL,
    /** The command line of Grails 9. */
    COMMAND_LINE
  }

  /** The options the plugin gives the command line in front of every command, when its document does not say otherwise. */
  public static final List<String> GLOBAL_OPTIONS = List.of(Grails9CommandExecutor.PLAIN_OUTPUT, Grails9CommandExecutor.NON_INTERACTIVE);

  private static final String NOTIFICATION_GROUP = "Grails command line";

  private static final Logger LOG = Logger.getInstance(Grails9CommandLines.class);

  private final @NotNull Project myProject;
  /** What each jar answered, once the notice of this project about it, if any, is given. */
  private final Map<Grails9CommandLineProbe.Jar, CompletableFuture<Grails9CommandLineProbe.Result>> myCapabilities = new ConcurrentHashMap<>();
  private final Set<Grails9CommandLineProbe.Jar> myNoticed = ConcurrentHashMap.newKeySet();

  public Grails9CommandLines(@NotNull Project project) {
    myProject = project;
  }

  public static @NotNull Grails9CommandLines getInstance(@NotNull Project project) {
    return project.getService(Grails9CommandLines.class);
  }

  /** @return whether the commands of {@code application} run in the command line of Grails 9; never waits */
  public static boolean runsCommandLine(@NotNull GrailsApplication application) {
    return getInstance(application.getProject()).runnerOf(application) == Runner.COMMAND_LINE;
  }

  /** @return whether the commands of {@code application} run in the Grails shell; never waits */
  public static boolean runsShell(@NotNull GrailsApplication application) {
    return getInstance(application.getProject()).runnerOf(application) == Runner.SHELL;
  }

  /**
   * @return what runs the commands of {@code application}. This never waits: while the jar of its command line is being
   * asked, the answer is {@link Runner#COMMAND_LINE}.
   */
  public @NotNull Runner runnerOf(@NotNull GrailsApplication application) {
    if (!Grails9Projects.isGrails9OrLater(application)) return Runner.SHELL;
    Grails3Application grails9 = (Grails3Application)application;
    return switch (choice()) {
      case COMMAND_LINE -> Runner.COMMAND_LINE;
      case SHELL -> Runner.SHELL;
      case AUTOMATIC -> automatic(grails9, capability(grails9).getNow(null));
    };
  }

  /**
   * @return what runs the commands of {@code application}, once its jar has answered. Off the event dispatch thread this
   * waits for that, cancellably; on it, it is {@link #runnerOf(GrailsApplication)}.
   */
  public @NotNull Runner awaitRunnerOf(@NotNull GrailsApplication application) {
    if (!ApplicationManager.getApplication().isDispatchThread()) {
      await(capability(application));
    }
    return runnerOf(application);
  }

  private static @NotNull Runner automatic(@NotNull Grails3Application application, @Nullable Grails9CommandLineProbe.Result result) {
    if (application.getGradleData().getShellUrls().isEmpty()) return Runner.COMMAND_LINE;
    if (jarOf(application) == null) return Runner.SHELL;
    return result == null || result.isCommandLine() ? Runner.COMMAND_LINE : Runner.SHELL;
  }

  /**
   * @return what the jar of the command line of {@code application} answered, once it has. It completes with
   * {@code null} at once when nothing is asked: for an application of an earlier version, one whose jar the import did
   * not resolve, and when the Grails shell is chosen.
   */
  public @NotNull CompletableFuture<Grails9CommandLineProbe.Result> capability(@NotNull GrailsApplication application) {
    if (!Grails9Projects.isGrails9OrLater(application) || choice() == GrailsCommandLineSettings.Choice.SHELL) {
      return CompletableFuture.completedFuture(null);
    }
    Grails3Application grails9 = (Grails3Application)application;
    Grails9CommandLineProbe.Jar jar = jarOf(grails9);
    if (jar == null) return CompletableFuture.completedFuture(null);
    return myCapabilities.computeIfAbsent(jar, key -> Grails9CommandLineProbe.getInstance().probe(key).thenApply(answer -> {
      try {
        noticeOf(grails9, key, answer);
      }
      catch (RuntimeException e) {
        LOG.warn("The notice about the Grails command line " + key.path() + " could not be given", e);
      }
      return answer;
    }));
  }

  /** Has the jar of the command line of every Grails application of the project asked, in the background. */
  public void prefetch() {
    for (GrailsApplication application : GrailsApplicationManager.getInstance(myProject).getApplications()) {
      capability(application);
    }
  }

  /**
   * @return the options to give the command line of {@code application} in front of a command: of
   * {@link #GLOBAL_OPTIONS}, those its document lists among its own options, or all of them while it has not answered
   * with a document that lists any
   */
  public @NotNull List<String> globalOptions(@NotNull GrailsApplication application) {
    Grails9CommandLineProbe.Result result = capability(application).getNow(null);
    List<String> listed = result == null || result.document() == null ? null : result.document().getOptionNames();
    if (listed == null) return GLOBAL_OPTIONS;
    List<String> options = new ArrayList<>();
    for (String option : GLOBAL_OPTIONS) {
      if (listed.contains(option)) options.add(option);
    }
    return options;
  }

  /**
   * Says, once for the project and the jar, that the commands of {@code application} could not be listed, so that names
   * are not completed.
   */
  void listingFailed(@NotNull GrailsApplication application, @NotNull String detail) {
    if (!(application instanceof Grails3Application grails9)) return;
    Grails9CommandLineProbe.Jar jar = jarOf(grails9);
    if (jar == null) return;
    notice(jar, NotificationType.WARNING,
           GrailsBundle.message("notification.content.grails9.listing.failed", application.getRoot().getPath(), detail));
  }

  private void noticeOf(@NotNull Grails3Application application,
                        @NotNull Grails9CommandLineProbe.Jar jar,
                        @NotNull Grails9CommandLineProbe.Result result) {
    if (result.kind() == Grails9CommandLineProbe.Kind.COMMAND_LINE || myProject.isDisposed()) return;
    GrailsCommandLineSettings.Choice choice = choice();
    if (choice == GrailsCommandLineSettings.Choice.SHELL) return;
    String version = application.getGradleData().getGrailsVersion();
    String jarName = new File(jar.path()).getName();
    if (result.kind() == Grails9CommandLineProbe.Kind.OTHER_FORMAT) {
      notice(jar, NotificationType.WARNING,
             GrailsBundle.message("notification.content.grails9.other.format", version, jarName, result.detail()));
    }
    else if (choice == GrailsCommandLineSettings.Choice.AUTOMATIC && !application.getGradleData().getShellUrls().isEmpty()) {
      notice(jar, NotificationType.INFORMATION,
             GrailsBundle.message("notification.content.grails9.shell.used", version, jarName, result.detail(),
                                  GrailsCommandLine.shellCoordinates(version)));
    }
    else if (choice == GrailsCommandLineSettings.Choice.AUTOMATIC) {
      notice(jar, NotificationType.WARNING,
             GrailsBundle.message("notification.content.grails9.no.shell", version, jarName, result.detail(),
                                  GrailsCommandLine.shellCoordinates(version)));
    }
    else {
      notice(jar, NotificationType.WARNING,
             GrailsBundle.message("notification.content.grails9.chosen.command.line", version, jarName, result.detail()));
    }
  }

  private void notice(@NotNull Grails9CommandLineProbe.Jar jar, @NotNull NotificationType type, @NotNull String content) {
    if (myProject.isDisposed() || !myNoticed.add(jar)) return;
    Notification notification = new Notification(NOTIFICATION_GROUP, GrailsBundle.message("notification.title.grails9.command.line"),
                                                 content, type);
    notification.addAction(NotificationAction.createSimple(
      GrailsBundle.message("notification.action.grails9.command.line.settings"),
      () -> ShowSettingsUtil.getInstance().showSettingsDialog(myProject, GrailsCustomFoldersConfigurable.class)));
    notification.notify(myProject);
  }

  private @NotNull GrailsCommandLineSettings.Choice choice() {
    return GrailsCommandLineSettings.getInstance(myProject).getChoice();
  }

  /** @return the jar of the command line the import resolved for {@code application}, or {@code null} when there is none on disk */
  private static @Nullable Grails9CommandLineProbe.Jar jarOf(@NotNull Grails3Application application) {
    for (String path : application.getGradleData().getCommandLineUrls()) {
      Grails9CommandLineProbe.Jar jar = Grails9CommandLineProbe.Jar.of(path);
      if (jar != null) return jar;
    }
    return null;
  }

  private static void await(@NotNull CompletableFuture<?> future) {
    while (!future.isDone()) {
      ProgressManager.checkCanceled();
      try {
        future.get(50, TimeUnit.MILLISECONDS);
      }
      catch (TimeoutException ignored) {
        // ask again, after a check for cancellation
      }
      catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new CancellationException("Interrupted while the Grails command line was asked what it is");
      }
      catch (java.util.concurrent.ExecutionException e) {
        return;
      }
    }
  }

  /** Asks the jars of the Grails applications of a project as soon as they are known. */
  public static final class Prefetch implements GrailsApplicationListener {
    private final @NotNull Project myProject;

    public Prefetch(@NotNull Project project) {
      myProject = project;
    }

    @Override
    public void applicationsRecomputed() {
      getInstance(myProject).prefetch();
    }
  }
}

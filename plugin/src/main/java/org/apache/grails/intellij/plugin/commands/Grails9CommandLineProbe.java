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
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.util.SystemProperties;
import com.intellij.util.concurrency.AppExecutorUtil;
import org.apache.grails.intellij.plugin.runner.impl.Grails9CommandExecutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Finds out whether a jar of {@code org.apache.grails:grails-cli} is the command line of Grails 9: whether it answers
 * {@code help --json} with a document whose {@code format} the plugin reads.
 * <p>
 * The version of a project does not tell it. Every build of Grails 9 from before its command line (the snapshots and
 * milestones until then) publishes a {@code grails-cli} jar whose start class hands the command to the Grails shell,
 * which knows no {@code help --json}; and the contract of the command line can still change before Grails 9.0.0. So
 * the jar is asked, once for each jar as it is on disk ({@link Jar}): its path, size and time of modification, so that
 * a jar replaced in place, as a snapshot in a local Maven repository is, is asked again.
 * <p>
 * The jar is started as a tool starts the command line, with {@code --plain-output --non-interactive help --json}, in an
 * empty directory and with the Java runtime of the IDE, which the command line of any version runs on. Its environment
 * has {@code GRAILS_PREFERRED_SHELL=forge}, which the command line of Grails 9 does not read; the start class of the
 * earlier builds reads it, and starts the Grails Forge CLI instead of the shell, which refuses the options at once
 * rather than resolve the profiles of the shell and start Gradle, as the shell does for a minute and more.
 * <p>
 * Nothing waits for it on the event dispatch thread: the answer is a future, computed on a pooled thread, and a jar
 * that does not answer within {@link #TIMEOUT_MILLIS} is stopped. The command line of Grails 9 answers in about a
 * second, since {@code help --json} starts nothing but the command line itself.
 */
@Service(Service.Level.APP)
public final class Grails9CommandLineProbe {

  private static final Logger LOG = Logger.getInstance(Grails9CommandLineProbe.class);

  /** How long the jar has to answer. */
  public static final int TIMEOUT_MILLIS = 30_000;

  /** The variable the start class of the builds of Grails 9 from before its command line reads, as Grails 7 and 8 do. */
  static final String PREFERRED_SHELL = "GRAILS_PREFERRED_SHELL";

  /** What a jar is. */
  public enum Kind {
    /** The command line of Grails 9: it answered {@code help --json} with a document of {@link Grails9HelpDocument#FORMAT}. */
    COMMAND_LINE,
    /** The command line of Grails 9 too, but its document is of another format, which this version of the plugin does not read. */
    OTHER_FORMAT,
    /** Not the command line of Grails 9: it ended without a document, or with one that is not of the command line. */
    NO_DOCUMENT,
    /** It did not answer within {@link #TIMEOUT_MILLIS}. */
    TIMED_OUT
  }

  /**
   * What a jar answered.
   *
   * @param document the document, for {@link Kind#COMMAND_LINE}
   * @param detail   for the other kinds, one line that says what went wrong, to show to the user
   */
  public record Result(@NotNull Kind kind, @Nullable Grails9HelpDocument document, @NotNull String detail) {

    /** @return whether the jar is the command line of Grails 9, whether or not its document can be read */
    public boolean isCommandLine() {
      return kind == Kind.COMMAND_LINE || kind == Kind.OTHER_FORMAT;
    }
  }

  /**
   * A jar as it is on disk.
   *
   * @param path the absolute path of the jar
   */
  public record Jar(@NotNull String path, long size, long lastModified) {

    /** @return the jar at {@code path} as it is now, or {@code null} when there is no such file */
    public static @Nullable Jar of(@NotNull String path) {
      File file = new File(path);
      if (!file.isFile()) return null;
      return new Jar(file.getAbsolutePath(), file.length(), file.lastModified());
    }
  }

  private final Map<Jar, CompletableFuture<Result>> myResults = new ConcurrentHashMap<>();
  private volatile int myTimeoutMillis = TIMEOUT_MILLIS;

  public static @NotNull Grails9CommandLineProbe getInstance() {
    return ApplicationManager.getApplication().getService(Grails9CommandLineProbe.class);
  }

  /** @return what {@code jar} answers; asked once for each jar, on a pooled thread */
  public @NotNull CompletableFuture<Result> probe(@NotNull Jar jar) {
    return myResults.computeIfAbsent(jar, key -> CompletableFuture.supplyAsync(() -> {
      try {
        return ask(key);
      }
      catch (RuntimeException e) {
        LOG.warn("The Grails command line " + key.path() + " could not be asked what it is", e);
        return new Result(Kind.NO_DOCUMENT, null, "it could not be asked: " + e);
      }
    }, AppExecutorUtil.getAppExecutorService()));
  }

  /** @return what {@code jar} answered, or {@code null} while it is being asked or when it was not */
  public @Nullable Result resultNow(@NotNull Jar jar) {
    CompletableFuture<Result> result = myResults.get(jar);
    return result == null ? null : result.getNow(null);
  }

  private @NotNull Result ask(@NotNull Jar jar) {
    File directory;
    try {
      directory = FileUtil.createTempDirectory("grails-cli-probe", null, true);
    }
    catch (IOException e) {
      return new Result(Kind.NO_DOCUMENT, null, "no directory to start it in: " + e.getMessage());
    }
    try {
      ProcessOutput output = ExecUtil.execAndGetOutput(commandLine(jar, directory), myTimeoutMillis);
      if (output.isTimeout()) {
        return new Result(Kind.TIMED_OUT, null, "help --json did not end within " + myTimeoutMillis / 1000 + " seconds");
      }
      if (output.getExitCode() != 0) {
        return new Result(Kind.NO_DOCUMENT, null, "help --json ended with exit code " + output.getExitCode() + firstLine(output.getStderr()));
      }
      try {
        return new Result(Kind.COMMAND_LINE, Grails9HelpDocument.parse(output.getStdout()), "");
      }
      catch (Grails9HelpDocument.OtherFormatException e) {
        return new Result(Kind.OTHER_FORMAT, null, "help --json answered a document of format " + e.getFormat());
      }
      catch (IOException | RuntimeException e) {
        return new Result(Kind.NO_DOCUMENT, null, "help --json answered no document" + firstLine(output.getStdout()));
      }
    }
    catch (ExecutionException e) {
      LOG.info("The Grails command line " + jar.path() + " could not be started", e);
      return new Result(Kind.NO_DOCUMENT, null, "it could not be started: " + e.getMessage());
    }
    finally {
      FileUtil.delete(directory);
    }
  }

  /** The command that asks {@code jar}, in {@code directory}. */
  static @NotNull GeneralCommandLine commandLine(@NotNull Jar jar, @NotNull File directory) {
    List<String> arguments = List.of(javaExecutable(), "-XX:TieredStopAtLevel=1", "-cp", jar.path(),
                                     Grails9CommandExecutor.mainClass(List.of(jar.path())),
                                     Grails9CommandExecutor.PLAIN_OUTPUT, Grails9CommandExecutor.NON_INTERACTIVE, "help", "--json");
    return new GeneralCommandLine(arguments)
      .withWorkDirectory(directory)
      .withEnvironment(PREFERRED_SHELL, "forge")
      .withCharset(StandardCharsets.UTF_8);
  }

  private static @NotNull String javaExecutable() {
    return new File(SystemProperties.getJavaHome(), "bin/" + (SystemInfo.isWindows ? "java.exe" : "java")).getPath();
  }

  private static @NotNull String firstLine(@NotNull String text) {
    String line = StringUtil.substringBefore(text.strip() + "\n", "\n");
    return line == null || line.isBlank() ? "" : ": " + StringUtil.shortenTextWithEllipsis(line.strip(), 200, 0);
  }

  /** Gives every jar {@code timeoutMillis} to answer, and forgets every answer, until {@code parentDisposable} is disposed. */
  @TestOnly
  public void setTimeout(int timeoutMillis, @NotNull Disposable parentDisposable) {
    myTimeoutMillis = timeoutMillis;
    myResults.clear();
    Disposer.register(parentDisposable, () -> {
      myTimeoutMillis = TIMEOUT_MILLIS;
      myResults.clear();
    });
  }
}

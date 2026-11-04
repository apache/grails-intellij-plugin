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

import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.projectRoots.JavaSdk;
import com.intellij.openapi.projectRoots.Sdk;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.testFramework.IdeaTestUtil;
import com.intellij.testFramework.LoggedErrorProcessor;
import com.intellij.testFramework.PlatformTestUtil;
import com.intellij.util.ThrowableRunnable;
import org.apache.grails.intellij.plugin.util.ControlFlowGuard;
import org.apache.grails.intellij.lib.testFramework.GrailsTestCase;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.nio.file.Path;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

public class GrailsConsoleTest extends GrailsTestCase {

  private GrailsConsole myConsole;

  /**
   * A command that cannot start, so {@code new OSProcessHandler} throws and {@code onDone} runs
   * down the failure path. That path needs no real process, which is what makes this test portable.
   */
  private static final GeneralCommandLine UNSTARTABLE =
    new GeneralCommandLine("no-such-grails-executable-for-this-test");

  // GrailsConsole builds a real ConsoleViewImpl, so this test needs a real JDK: the minimal
  // Mock JDK 11 omits the AWT/Swing classes the console view is made of.
  @Override
  protected @NotNull Supplier<Sdk> getTestJdk() {
    return () -> JavaSdk.getInstance().createJdk("TEST_JDK", IdeaTestUtil.requireRealJdkHome(), false);
  }

  @Override
  protected void tearDown() throws Exception {
    // The console is a project service, so a light project would keep its editor alive past this
    // test and fail the fixture's release check. Disposing it here releases the console view.
    try {
      if (myConsole != null) {
        Disposer.dispose(myConsole);
        myConsole = null;
      }
    }
    finally {
      super.tearDown();
    }
  }

  /**
   * The guard has to propagate the cancellation, but not at the price of the state the surrounding
   * catch block owns: leaving {@code myExecuting} set is unrecoverable, because every later command
   * queues behind a queue nothing drains again.
   */
  public void testCancellationFromOnDoneStillReleasesTheConsole() throws RuntimeException {
    myConsole = GrailsConsole.getInstance(getProject());
    GrailsConsole console = myConsole;
    assertFalse("precondition: the console must start idle", console.isExecuting());

    ProcessCanceledException cancelled = new ProcessCanceledException();
    AtomicBoolean propagated = new AtomicBoolean();

    // handleExecutionError logs the unstartable command; that is the fixture, not the behaviour
    // under test, so it is swallowed here rather than failing the test for the wrong reason.
    LoggedErrorProcessor.executeWith(new LoggedErrorProcessor() {
      @Override
      public @NotNull Set<Action> processError(@NotNull String category, @NotNull String message, String[] details, Throwable t) {
        return EnumSet.noneOf(Action.class);
      }
    }, (ThrowableRunnable<RuntimeException>)() -> {
      try {
        console.executeProcess(UNSTARTABLE, () -> {
          throw cancelled;
        }, false, true);
      }
      catch (ProcessCanceledException pce) {
        assertSame("the cancellation must propagate unchanged", cancelled, pce);
        propagated.set(true);
        return;
      }
      throw new AssertionError("expected the cancellation thrown by onDone() to propagate");
    });

    assertTrue("the cancellation thrown by onDone() must reach the caller", propagated.get());
    assertFalse("a propagated cancellation must still release the console, or every later command queues forever",
                console.isExecuting());
  }

  /**
   * A real process that exits 0, whose {@code onDone} throws a control-flow exception. The
   * cancellation propagates (to the EDT, via the completion callback), but the console teardown
   * and queue release must happen in a {@code finally}, or the console stays busy forever.
   */
  public void testCancellationFromOnDoneOnSuccessStillReleasesTheConsole() throws Exception {
    myConsole = GrailsConsole.getInstance(getProject());
    GrailsConsole console = myConsole;
    assertFalse("precondition: the console must start idle", console.isExecuting());

    Path javaBin = Path.of(IdeaTestUtil.requireRealJdkHome(), "bin", SystemInfo.isWindows ? "java.exe" : "java");
    GeneralCommandLine javaVersion = new GeneralCommandLine(javaBin.toString(), "-version");

    AtomicReference<Throwable> unexpected = new AtomicReference<>();
    LoggedErrorProcessor.executeWith(new LoggedErrorProcessor() {
      @Override
      public @NotNull Set<Action> processError(@NotNull String category, @NotNull String message, String[] details, Throwable t) {
        // The cancellation thrown by onDone() is rethrown on the EDT; that is the behaviour
        // under test, so it is swallowed here. Anything else fails the test for the right reason.
        if (ControlFlowGuard.isControlFlow(t)) {
          return EnumSet.noneOf(Action.class);
        }
        unexpected.compareAndSet(null, t);
        return EnumSet.noneOf(Action.class);
      }
    }, (ThrowableRunnable<RuntimeException>)() -> {
      console.executeProcess(javaVersion, () -> {
        throw new ProcessCanceledException();
      }, false, false);
    });

    long deadline = System.currentTimeMillis() + 15000;
    while (console.isExecuting() && System.currentTimeMillis() < deadline) {
      PlatformTestUtil.dispatchAllEventsInIdeEventQueue();
    }

    assertNull("unexpected error while waiting for process completion: " + unexpected.get(), unexpected.get());
    assertFalse("a cancellation thrown by onDone() on success must still release the console, "
                + "or every later command queues forever",
                console.isExecuting());

    // The kill handler must be cleared too: with the teardown outside a finally, a control-flow
    // exception from onDone() skips it and the console keeps offering to kill a dead process.
    Field killActionField = GrailsConsole.class.getDeclaredField("myKillAction");
    killActionField.setAccessible(true);
    Object killAction = killActionField.get(console);
    assertFalse("the kill handler must be cleared even when onDone() throws cancellation",
                (Boolean)killAction.getClass().getMethod("isEnabled").invoke(killAction));
  }

  private static GeneralCommandLine java(String... arguments) {
    Path javaBin = Path.of(IdeaTestUtil.requireRealJdkHome(), "bin", SystemInfo.isWindows ? "java.exe" : "java");
    return new GeneralCommandLine(javaBin.toString()).withParameters(arguments);
  }

  /** Pumps the EDT until the console has released its reservation: completion runs there. */
  private static void awaitIdle(GrailsConsole console) {
    PlatformTestUtil.waitWithEventsDispatching("the console did not go idle", () -> !console.isExecuting(), 30);
  }

  private static boolean isKillActionEnabled(GrailsConsole console) throws Exception {
    Field killActionField = GrailsConsole.class.getDeclaredField("myKillAction");
    killActionField.setAccessible(true);
    Object killAction = killActionField.get(console);
    return (Boolean)killAction.getClass().getMethod("isEnabled").invoke(killAction);
  }

  /**
   * A process that exits with a non-zero code used to leave {@code myExecuting} set forever: the
   * waiter returned without releasing, so every later command queued silently behind it.
   */
  public void testFailedProcessReleasesTheConsoleWithoutRunningOnDone() throws Exception {
    myConsole = GrailsConsole.getInstance(getProject());
    GrailsConsole console = myConsole;
    assertFalse("precondition: the console must start idle", console.isExecuting());

    AtomicBoolean onDoneRan = new AtomicBoolean();
    console.executeProcess(java("-no-such-option"), () -> onDoneRan.set(true), false, false);
    assertTrue("the console is reserved until the completion runs on the EDT", console.isExecuting());

    awaitIdle(console);
    assertFalse("onDone must not run for a command that failed", onDoneRan.get());
    assertFalse("the kill handler must be cleared once the process is gone", isKillActionEnabled(console));
  }

  /**
   * Artefact creation submits from a pooled thread: the modality falls back to non-modal, the
   * console attaches through invokeAndWait, and onDone still runs on the EDT like for EDT callers.
   */
  public void testProcessSubmittedFromAPooledThreadRunsOnDoneOnTheEdt() throws Exception {
    myConsole = GrailsConsole.getInstance(getProject());
    GrailsConsole console = myConsole;

    AtomicBoolean onDoneRan = new AtomicBoolean();
    AtomicBoolean onDoneOnEdt = new AtomicBoolean();
    Future<?> submitted = ApplicationManager.getApplication().executeOnPooledThread(
      () -> console.executeProcess(java("-version"), () -> {
        onDoneOnEdt.set(ApplicationManager.getApplication().isDispatchThread());
        onDoneRan.set(true);
      }, false, false));

    // The submitting thread needs the EDT (invokeAndWait for the console attachment), so pump it.
    PlatformTestUtil.waitWithEventsDispatching("the background submission did not complete", submitted::isDone, 30);
    submitted.get();
    awaitIdle(console);

    assertTrue("onDone must run after a successful command", onDoneRan.get());
    assertTrue("onDone must run on the EDT, its callers touch PSI, VFS and UI", onDoneOnEdt.get());
    assertFalse("the kill handler must be cleared once the process is gone", isKillActionEnabled(console));
  }

  /**
   * Only an ExecutionException used to release the reservation. Anything else thrown while
   * starting the process escaped with the console still busy, and on the background path nothing
   * upstream could notice.
   */
  public void testExceptionWhileStartingTheProcessReleasesTheConsole() {
    myConsole = GrailsConsole.getInstance(getProject());
    GrailsConsole console = myConsole;

    GeneralCommandLine exploding = new GeneralCommandLine("java") {
      @Override
      public @NotNull Process createProcess() {
        throw new IllegalStateException("cannot start");
      }
    };
    try {
      console.executeProcess(exploding, null, false, false);
      fail("expected the exception thrown while starting the process to propagate");
    }
    catch (IllegalStateException e) {
      assertEquals("cannot start", e.getMessage());
    }

    assertFalse("a failure before the process runs must still release the console", console.isExecuting());
  }

  /** The second command waits for the first and starts once it completed; the console ends up idle. */
  public void testQueuedProcessRunsAfterTheCurrentOne() {
    myConsole = GrailsConsole.getInstance(getProject());
    GrailsConsole console = myConsole;

    List<String> order = new CopyOnWriteArrayList<>();
    console.executeProcess(java("-version"), () -> order.add("first"), false, false);
    console.executeProcess(java("-version"), () -> order.add("second"), false, false);
    assertEquals("the second command queues behind the first", List.of(), order);

    awaitIdle(console);
    assertEquals(List.of("first", "second"), order);
  }
}

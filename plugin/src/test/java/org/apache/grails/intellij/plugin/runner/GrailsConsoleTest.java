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
import java.util.Set;
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
}

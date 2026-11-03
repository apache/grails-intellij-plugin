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
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.filters.TextConsoleBuilderFactory;
import com.intellij.execution.impl.ConsoleViewImpl;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessListener;
import com.intellij.execution.process.ProcessOutputTypes;
import com.intellij.execution.runners.ExecutionUtil;
import com.intellij.execution.ui.ConsoleViewContentType;
import com.intellij.execution.ui.RunnerLayoutUi;
import com.intellij.execution.ui.layout.PlaceInGrid;
import com.intellij.icons.AllIcons;
import com.intellij.notification.NotificationGroup;
import com.intellij.notification.NotificationGroupManager;
import com.intellij.openapi.Disposable;
import com.intellij.openapi.actionSystem.ActionUpdateThread;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.DefaultActionGroup;
import com.intellij.openapi.actionSystem.ToggleAction;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileDocumentManager;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.wm.ToolWindowAnchor;
import com.intellij.openapi.wm.ToolWindowManager;
import com.intellij.openapi.wm.ex.ToolWindowEx;
import com.intellij.ui.content.Content;
import com.intellij.ui.content.ContentFactory;
import com.intellij.ui.content.ContentManager;
import com.intellij.util.DisposeAwareRunnable;
import com.intellij.util.containers.ContainerUtil;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.mvc.ConsoleProcessDescriptor;
import org.apache.grails.intellij.plugin.util.ControlFlowGuard;

import javax.swing.JComponent;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import kotlin.Unit;

@Service(Service.Level.PROJECT)
@State(name = "GrailsConsole", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class GrailsConsole implements Disposable, PersistentStateComponent<GrailsConsoleState> {
  //private static final Key<Boolean> UPDATING_BY_CONSOLE_PROCESS = Key.create("UPDATING_BY_CONSOLE_PROCESS");
  private static final Logger LOG = Logger.getInstance(GrailsConsole.class);

  public static final @NonNls String TOOL_WINDOW_ID = "Grails Console";
  public static final NotificationGroup NOTIFICATION_GROUP = NotificationGroupManager.getInstance().getNotificationGroup("Grails notifications");

  private final Project myProject;
  private final ConsoleViewImpl myConsole;
  private final ToolWindowEx myToolWindow;
  private final Content myContent;
  private volatile boolean myIsAutoCloseEnabled = true;
  //private final JPanel myPanel = new JPanel(new BorderLayout());
  private final Queue<MyProcessInConsole> myProcessQueue = new LinkedList<>();
  private final MyKillProcessAction myKillAction = new MyKillProcessAction();

  private final Object myLock = new Object();
  private volatile boolean myExecuting = false;

  public GrailsConsole(Project project) {
    myProject = project;
    myConsole = (ConsoleViewImpl)TextConsoleBuilderFactory.getInstance().createBuilder(myProject).getConsole();
    Disposer.register(this, myConsole);

    myToolWindow = (ToolWindowEx)ToolWindowManager.getInstance(myProject).registerToolWindow(TOOL_WINDOW_ID, builder -> {
      builder.anchor = ToolWindowAnchor.BOTTOM;
      builder.canCloseContent = false;
      builder.icon = GroovyMvcIcons.Grails_13;
      return Unit.INSTANCE;
    });
    myToolWindow.setAdditionalGearActions(new DefaultActionGroup(new ToggleAction(GrailsBundle.message("action.text.auto.close.when.done")) {

      @Override
      public @NotNull ActionUpdateThread getActionUpdateThread() {
        return ActionUpdateThread.BGT;
      }

      @Override
      public boolean isSelected(@NotNull AnActionEvent e) {
        return myIsAutoCloseEnabled;
      }

      @Override
      public void setSelected(@NotNull AnActionEvent e, boolean state) {
        myIsAutoCloseEnabled = state;
      }
    }));

    //Create runner UI layout
    final RunnerLayoutUi.Factory factory = RunnerLayoutUi.Factory.getInstance(myProject);
    final RunnerLayoutUi layoutUi = factory.create("", "", "session", this);
    final JComponent uiComponent = layoutUi.getComponent();

    // Adding actions
    DefaultActionGroup group = new DefaultActionGroup();
    group.add(myKillAction);
    group.addSeparator();

    layoutUi.getOptions().setLeftToolbar(group, "GrailsConsoleToolbar");

    final Content console = layoutUi.createContent("Grails Console 111", myConsole.getComponent(), "", null, null);
    console.setCloseable(false);
    layoutUi.addContent(console, 0, PlaceInGrid.right, false);

    //myPanel.add(uiComponent, BorderLayout.CENTER);

    final ContentManager manager = myToolWindow.getContentManager();
    final ContentFactory contentFactory = ContentFactory.getInstance();
    myContent = contentFactory.createContent(uiComponent, null, true);
    manager.addContent(myContent);
  }

  @Override
  public @NotNull GrailsConsoleState getState() {
    return new GrailsConsoleState(myIsAutoCloseEnabled);
  }

  @Override
  public void loadState(@NotNull GrailsConsoleState state) {
    myIsAutoCloseEnabled = state.getAutoCloseEnabled();
  }

  public static GrailsConsole getInstance(@NotNull Project project) {
    return project.getService(GrailsConsole.class);
  }

  //public static boolean isUpdatingVfsByConsoleProcess(@NotNull Module module) {
  //  Boolean flag = module.getUserData(UPDATING_BY_CONSOLE_PROCESS);
  //  return flag != null && flag;
  //}

  public void show(final @Nullable Runnable runnable, boolean focus) {
    Runnable r = null;
    if (runnable != null) {
      r = DisposeAwareRunnable.create(runnable, myProject);
    }

    myToolWindow.activate(r, focus);
  }

  private static class MyProcessInConsole implements ConsoleProcessDescriptor {
    final GeneralCommandLine commandLine;
    final @Nullable Runnable onDone;
    final boolean closeOnDone;
    final boolean showConsole;
    final String[] input;
    private final List<ProcessListener> myListeners = ContainerUtil.createLockFreeCopyOnWriteList();

    private OSProcessHandler myHandler;

    MyProcessInConsole(final GeneralCommandLine commandLine,
                       final @Nullable Runnable onDone,
                       final boolean showConsole,
                       final boolean closeOnDone,
                       final String[] input) {
      this.commandLine = commandLine;
      this.onDone = onDone;
      this.closeOnDone = closeOnDone;
      this.input = input;
      this.showConsole = showConsole;
    }

    @Override
    public ConsoleProcessDescriptor addProcessListener(@NotNull ProcessListener listener) {
      if (myHandler != null) {
        myHandler.addProcessListener(listener);
      }
      else {
        myListeners.add(listener);
      }
      return this;
    }

    @Override
    public ConsoleProcessDescriptor waitWith(ProgressIndicator progressIndicator) {
      if (myHandler != null) {
        doWait(progressIndicator);
      }
      return this;
    }

    private void doWait(ProgressIndicator progressIndicator) {
      while (!myHandler.waitFor(500)) {
        if (progressIndicator.isCanceled()) {
          myHandler.destroyProcess();
          break;
        }
      }
    }

    public void setHandler(OSProcessHandler handler) {
      myHandler = handler;
      for (final ProcessListener listener : myListeners) {
        handler.addProcessListener(listener);
      }
    }
  }

  public static @NotNull ConsoleProcessDescriptor executeProcess(Project project,
                                                                 final GeneralCommandLine commandLine,
                                                                 final @Nullable Runnable onDone,
                                                                 final boolean closeOnDone,
                                                                 final String... input) {
    return getInstance(project).executeProcess(commandLine, onDone, true, closeOnDone, input);
  }

  public @NotNull ConsoleProcessDescriptor executeProcess(final GeneralCommandLine commandLine,
                                                          final @Nullable Runnable onDone,
                                                          boolean showConsole,
                                                          final boolean closeOnDone,
                                                          final String... input) {
    // Relaxed: no longer asserts EDT so artefact creation can hand off to a background thread.
    // Queueing/flag transitions are guarded by myLock to stay thread-safe.
    final MyProcessInConsole process = new MyProcessInConsole(commandLine, onDone, showConsole, closeOnDone, input);
    boolean startNow;
    synchronized (myLock) {
      if (myExecuting) {
        myProcessQueue.add(process);
        startNow = false;
      }
      else {
        myExecuting = true;
        startNow = true;
      }
    }
    if (startNow) {
      executeProcessImpl(process, true);
    }
    return process;
  }

  public boolean isExecuting() {
    return myExecuting;
  }

  private void executeProcessImpl(final MyProcessInConsole pic, boolean toFocus) {
    synchronized (myLock) {
      myExecuting = true;
    }
    final GeneralCommandLine commandLine = pic.commandLine;
    final String[] input = pic.input;
    final Runnable onDone = pic.onDone;

    // ModalityState.current() is EDT-only; when invoked off the EDT (artefact creation) we are
    // in a non-modal context, so fall back to nonModal() instead of asserting the EDT.
    final ModalityState modalityState = ApplicationManager.getApplication().isDispatchThread()
                                       ? ModalityState.current()
                                       : ModalityState.nonModal();
    final boolean modalContext = modalityState != ModalityState.nonModal();

    LOG.debug("Starting Grails process: " + commandLine.getCommandLineString());

    // Defer tool-window activation off the EDT-critical path for background callers so that
    // VCS "Changes" view layout handling (write-intent wait) does not run synchronously here.
    if (!modalContext && pic.showConsole) {
      if (ApplicationManager.getApplication().isDispatchThread()) {
        show(null, toFocus);
      }
      else {
        ApplicationManager.getApplication().invokeLater(() -> {
          if (myProject.isDisposed()) return;
          show(null, toFocus);
        }, modalityState);
      }
    }

    // saveAllDocuments runs on the calling thread: off the EDT when invoked from a background
    // thread (artefact creation), on the EDT for the existing callers that keep EDT behaviour.
    FileDocumentManager.getInstance().saveAllDocuments();
    final OSProcessHandler handler;
    try {
      handler = new OSProcessHandler(commandLine);

      @SuppressWarnings("IOResourceOpenedButNotSafelyClosed")
      OutputStreamWriter writer = new OutputStreamWriter(handler.getProcess().getOutputStream(), StandardCharsets.UTF_8);
      try {
        for (String s : input) {
          writer.write(s);
        }
        writer.flush();
      }
      catch (IOException e) {
        LOG.info("Cannot write Grails process input", e);
      }

      final Ref<Boolean> gotError = new Ref<>(false);
      handler.addProcessListener(new ProcessListener() {
        @Override
        public void onTextAvailable(@NotNull ProcessEvent event, @NotNull Key key) {
          if (key == ProcessOutputTypes.STDERR) gotError.set(true);
          LOG.debug("got text: " + event.getText());
        }

        @Override
        public void processTerminated(@NotNull ProcessEvent event) {
          final int exitCode = event.getExitCode();
          if (exitCode == 0 && !gotError.get().booleanValue()) {
            ApplicationManager.getApplication().invokeLater(() -> {
              if (myProject.isDisposed()) return;
              if (pic.closeOnDone && myIsAutoCloseEnabled) myToolWindow.hide(null);
            }, modalityState);
          }
        }
      });
    }
    catch (final ExecutionException ex) {
      try {
        ExecutionUtil.handleExecutionError(myProject, TOOL_WINDOW_ID, "Grails", ex);
        LOG.info(ex);
        try {
          if (onDone != null) {
            // The success path runs onDone on the EDT via finishProcess; the failure path must
            // honour the same contract (callers touch PSI/VFS/UI), while still propagating
            // control-flow exceptions to the caller.
            if (ApplicationManager.getApplication().isDispatchThread()) {
              onDone.run();
            }
            else {
              ApplicationManager.getApplication().invokeAndWait(onDone, modalityState);
            }
          }
        }
        catch (Exception e) {
          // A cancellation is not a failure, so it propagates - but the release below must happen
          // either way, or the console stays busy and every later command queues forever.
          ControlFlowGuard.rethrowIfControlFlow(e);
          LOG.error(e);
        }
      }
      finally {
        startNextOrRelease();
      }
      return;
    }

    pic.setHandler(handler);
    myKillAction.setHandler(handler);

    Runnable uiSetup = () -> {
      if (myProject.isDisposed()) return;
      myContent.setDisplayName(GrailsBundle.message("content.display.name.grails.executing"));
      myConsole.scrollToEnd();
      myConsole.attachToProcess(handler);
    };
    if (ApplicationManager.getApplication().isDispatchThread()) {
      uiSetup.run();
    }
    else {
      ApplicationManager.getApplication().invokeAndWait(uiSetup, modalityState);
    }

    ApplicationManager.getApplication().executeOnPooledThread(() -> {
      LOG.debug("Notifying Grails process handler");
      handler.startNotify();
      if (!handler.waitFor()) {
        // Killed or otherwise did not terminate normally: release the console and drain the
        // queue without running onDone, so later commands are not stuck behind a dead process.
        finishProcess(null, modalityState);
        return;
      }

      LOG.debug("Grails process finished with exit code " + handler.getExitCode());

      Integer exitCode = handler.getExitCode(); // wait until CLI creates content of directory
      if (exitCode == null || exitCode != 0) {
        // Something went wrong: release the console and drain the queue without running onDone.
        finishProcess(null, modalityState);
        return;
      }

      // Success: run onDone and release on the EDT. No VFS refresh here: every onDone that
      // needs fresh files refreshes exactly its own scope (e.g. NewGrailsXXXAction refreshes only
      // the generated files), and anything else picks new files up through the filesystem watcher.
      // Even a work-directory-scoped recursive refresh pushed every change underneath through the
      // RefreshQueue's read+write session, starving the EDT's write-intent permit (freeze).
      finishProcess(onDone, modalityState);
    });
  }

  /**
   * Runs {@code onDone} (unless the project is gone) and then always releases the console and
   * starts the next queued process. Every completion path funnels through here so a killed,
   * failed or orphaned process can never leave the console busy forever.
   */
  private void finishProcess(@Nullable Runnable onDone, ModalityState modalityState) {
    ApplicationManager.getApplication().invokeLater(() -> {
      try {
        if (!myProject.isDisposed()) {
          try {
            if (onDone != null) onDone.run();
          }
          catch (Exception e) {
            // A cancellation is not a failure, so it propagates - but the teardown below must
            // happen either way, or the console stays busy and the next queued command never
            // starts (hence the outer finally, not just a catch).
            ControlFlowGuard.rethrowIfControlFlow(e);
            LOG.error(e);
          }
          finally {
            myConsole.print("\n", ConsoleViewContentType.NORMAL_OUTPUT);
            myKillAction.setHandler(null);
            myContent.setDisplayName("");
          }
        }
      }
      finally {
        startNextOrRelease();
      }
    }, modalityState);
  }

  /**
   * Starts the next queued process, keeping the console reserved across the handoff, or releases
   * the console when the queue is empty. The reservation must be held: clearing
   * {@code myExecuting} before starting the polled process lets a concurrent submitter observe
   * idle and start a second process alongside the queued one.
   */
  private void startNextOrRelease() {
    final MyProcessInConsole next;
    synchronized (myLock) {
      if (myProject.isDisposed()) {
        myProcessQueue.clear();
        myExecuting = false;
        return;
      }
      next = myProcessQueue.poll();
      if (next == null) {
        myExecuting = false;
      }
      // else: myExecuting stays true across the handoff below.
    }
    if (next != null) {
      executeProcessImpl(next, false);
    }
  }

  @Override
  public void dispose() {
  }

  private class MyKillProcessAction extends AnAction {
    private OSProcessHandler myHandler = null;

    MyKillProcessAction() {
      super(GrailsBundle.message("action.MyKillProcessAction.description"), GrailsBundle.message("action.MyKillProcessAction.description"), AllIcons.Debugger.KillProcess);
    }

    public void setHandler(@Nullable OSProcessHandler handler) {
      myHandler = handler;
    }

    @Override
    public @NotNull ActionUpdateThread getActionUpdateThread() {
      return ActionUpdateThread.BGT;
    }

    @Override
    public void update(final @NotNull AnActionEvent e) {
      e.getPresentation().setEnabled(isEnabled());
    }

    @Override
    public void actionPerformed(final @NotNull AnActionEvent e) {
      if (myHandler != null) {
        final Process process = myHandler.getProcess();
        process.destroy();
        myConsole.print("Process terminated", ConsoleViewContentType.ERROR_OUTPUT);
      }
    }

    public boolean isEnabled() {
      return myHandler != null;
    }
  }

  public ConsoleViewImpl getConsole() {
    return myConsole;
  }
}

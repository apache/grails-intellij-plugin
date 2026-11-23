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

package org.apache.grails.intellij.plugin.config;

import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.openapi.components.StoragePathMacros;
import com.intellij.openapi.project.Project;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Which command line runs the commands of this project's Grails applications of Grails 9 and later, set on the Grails
 * settings page. The applications of earlier versions always run the Grails shell.
 * <p>
 * Grails 9 has one command line, {@code org.apache.grails:grails-cli}, and no shell; the builds of Grails 9 from before
 * that command line (snapshots and milestones) still have the shell. By default the plugin asks the jar of
 * {@code grails-cli} which of the two it is ({@link Choice#AUTOMATIC}); the other two choices are for when that answer
 * is not the one wanted. The setting is kept in the workspace of the project, as the project's own Grails view folders
 * are.
 */
@Service(Service.Level.PROJECT)
@State(name = "GrailsCommandLine", storages = @Storage(StoragePathMacros.WORKSPACE_FILE))
public final class GrailsCommandLineSettings implements PersistentStateComponent<GrailsCommandLineSettings.StateHolder> {

  /** The command line of the applications of Grails 9 and later. */
  public enum Choice {
    /**
     * The command line of Grails 9 when its jar answers {@code help --json} as that command line does, otherwise the
     * Grails shell of the version, when the Gradle import resolved one.
     */
    AUTOMATIC,
    /** The command line of Grails 9, whatever its jar answers. */
    COMMAND_LINE,
    /** The Grails shell of the version, as for Grails 8. */
    SHELL
  }

  /** Told when the choice changes. */
  public interface Listener {
    @Topic.ProjectLevel
    Topic<Listener> TOPIC = Topic.create("Grails command line choice", Listener.class);

    void choiceChanged();
  }

  private final @NotNull Project myProject;
  private volatile @NotNull Choice myChoice = Choice.AUTOMATIC;

  public GrailsCommandLineSettings(@NotNull Project project) {
    myProject = project;
  }

  public static @NotNull GrailsCommandLineSettings getInstance(@NotNull Project project) {
    return project.getService(GrailsCommandLineSettings.class);
  }

  public @NotNull Choice getChoice() {
    return myChoice;
  }

  public void setChoice(@NotNull Choice choice) {
    if (myChoice == choice) return;
    myChoice = choice;
    myProject.getMessageBus().syncPublisher(Listener.TOPIC).choiceChanged();
  }

  @Override
  public @NotNull StateHolder getState() {
    StateHolder state = new StateHolder();
    state.choice = myChoice.name();
    return state;
  }

  @Override
  public void loadState(@NotNull StateHolder state) {
    myChoice = choice(state.choice);
  }

  /** @return the choice named {@code name}, or {@link Choice#AUTOMATIC} for a name this version does not know */
  static @NotNull Choice choice(@Nullable String name) {
    for (Choice choice : Choice.values()) {
      if (choice.name().equals(name)) return choice;
    }
    return Choice.AUTOMATIC;
  }

  public static final class StateHolder {
    public String choice = Choice.AUTOMATIC.name();
  }
}

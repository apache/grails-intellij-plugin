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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.PersistentStateComponent;
import com.intellij.openapi.components.State;
import com.intellij.openapi.components.Storage;
import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@State(name = "GrailsCustomFolders", storages = @Storage("GrailsCustomFolders.xml"))
public final class GrailsCustomFoldersAppService implements PersistentStateComponent<GrailsCustomFoldersAppService.StateHolder> {

  public static final int DEFAULT_MAX_FOLDERS = 6;
  public static final int MIN_FOLDERS = 1;
  public static final int MAX_FOLDERS = 20;

  private List<String> folders = new ArrayList<>();
  private int maxFolders = DEFAULT_MAX_FOLDERS;

  public static @NotNull GrailsCustomFoldersAppService getStaticInstance() {
    return ApplicationManager.getApplication().getService(GrailsCustomFoldersAppService.class);
  }

  public @NotNull List<String> getFolders() {
    return Collections.unmodifiableList(new ArrayList<>(folders));
  }

  public void setFolders(@NotNull List<String> folders) {
    this.folders = clean(folders);
    publishGlobalFoldersChanged();
  }

  /** How many folders the Grails view renders in total; the merged list is truncated to it. */
  public int getMaxFolders() {
    return maxFolders;
  }

  public void setMaxFolders(int maxFolders) {
    this.maxFolders = clamp(maxFolders);
    publishGlobalFoldersChanged();
  }

  /**
   * Writes both global fields and publishes once. A settings page edits the folder list and the cap
   * together, and publishing per field would rebuild every open Grails pane once per field instead
   * of once per Apply.
   */
  public void setGlobalDefaults(@NotNull List<String> folders, int maxFolders) {
    this.folders = clean(folders);
    this.maxFolders = clamp(maxFolders);
    publishGlobalFoldersChanged();
  }

  private static void publishGlobalFoldersChanged() {
    ApplicationManager.getApplication().getMessageBus()
      .syncPublisher(GlobalFoldersChangedListener.TOPIC).globalFoldersChanged();
  }

  /** Clamps into the configured range, so a caller outside it cannot corrupt the setting. */
  static int clamp(int maxFolders) {
    return Math.max(MIN_FOLDERS, Math.min(MAX_FOLDERS, maxFolders));
  }

  static @NotNull List<String> clean(@NotNull List<String> raw) {
    return FolderPaths.dedupe(raw);
  }

  /**
   * A state file written before the cap became a setting has no {@code maxFolders} field, which
   * deserializes to 0, and a hand-edited file can hold anything else: anything outside the range
   * means "unset" and falls back to the default rather than to a bound.
   */
  static int restore(int storedMaxFolders) {
    return storedMaxFolders < MIN_FOLDERS || storedMaxFolders > MAX_FOLDERS
           ? DEFAULT_MAX_FOLDERS
           : storedMaxFolders;
  }

  @Override
  public StateHolder getState() {
    StateHolder res = new StateHolder();
    res.folders = new ArrayList<>(folders);
    res.maxFolders = maxFolders;
    return res;
  }

  @Override
  public void loadState(@NotNull StateHolder state) {
    folders = clean(state.folders);
    maxFolders = restore(state.maxFolders);
  }

  public static final class StateHolder {
    public List<String> folders = new ArrayList<>();
    public int maxFolders;
  }

  public interface GlobalFoldersChangedListener {

    Topic<GlobalFoldersChangedListener> TOPIC =
      Topic.create("Grails Global Custom Folders Changed", GlobalFoldersChangedListener.class);

    void globalFoldersChanged();
  }
}

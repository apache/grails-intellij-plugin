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
package org.apache.grails.intellij.plugin.actions;

import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsCustomFolder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The selection the custom-folder actions act on, read out of the data context. All three actions
 * used to carry their own copy of these two readers, and they drifted: one took the read action the
 * background-thread {@code update()} needs and the other did not, so the same data context threw in
 * one action and worked in the other. One copy is the only place that can happen again.
 */
final class CustomFolderSelection {

  private CustomFolderSelection() {
  }

  /**
   * The selected directory: as a PSI element when the pane snapshots one, or resolved from the file
   * when it snapshots only that.
   */
  static @Nullable PsiDirectory directory(@NotNull AnActionEvent e) {
    PsiElement element = e.getData(CommonDataKeys.PSI_ELEMENT);
    if (element instanceof PsiDirectory) {
      return (PsiDirectory)element;
    }
    Project project = e.getProject();
    VirtualFile virtualFile = e.getData(CommonDataKeys.VIRTUAL_FILE);
    if (project == null || virtualFile == null) {
      return null;
    }
    // The pane does not always snapshot a PSI element, so the file is resolved here; update() runs on
    // BGT, which holds no read action, and findDirectory queries PSI.
    return ReadAction.computeBlocking(() -> virtualFile.isDirectory()
                                             ? PsiManager.getInstance(project).findDirectory(virtualFile)
                                             : null);
  }

  /**
   * The last node in the pane's {@code SELECTED_ITEMS} snapshot that stands for a custom folder, or
   * null when the snapshot holds none. The snapshot holds the tree's user objects and is written on
   * the EDT, so this is safe from a background {@code update()}.
   */
  static @Nullable GrailsCustomFolder customFolder(@NotNull AnActionEvent e) {
    Object[] items = e.getData(PlatformCoreDataKeys.SELECTED_ITEMS);
    if (items == null) return null;
    for (int i = items.length - 1; i >= 0; i--) {
      if (items[i] instanceof GrailsCustomFolder folder) return folder;
    }
    return null;
  }
}

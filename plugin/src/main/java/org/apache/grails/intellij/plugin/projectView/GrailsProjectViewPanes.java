/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.grails.intellij.plugin.projectView;

import com.intellij.ide.projectView.ProjectView;
import com.intellij.ide.projectView.impl.AbstractProjectViewPane;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.config.GrailsConstants;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;

import java.util.Objects;

/** Was showHide.kt; adds or removes the Grails project view pane as applications appear or go away. */
public final class GrailsProjectViewPanes {

  static final String ID = GrailsConstants.GRAILS;

  private GrailsProjectViewPanes() {
  }

  public static void showHide(@NotNull Project project) {
    GrailsApplicationManager grailsApplicationManager = GrailsApplicationManager.getInstance(project);
    ProjectView projectView = ProjectView.getInstance(project);
    boolean hasPane = projectView.getPaneIds().contains(ID);
    if (grailsApplicationManager.hasApplications()) {
      if (!hasPane) {
        projectView.addProjectPane(getPane(project));
      }
    }
    else if (hasPane) {
      projectView.removeProjectPane(getPane(project));
    }
  }

  private static @NotNull AbstractProjectViewPane getPane(@NotNull Project project) {
    return Objects.requireNonNull(AbstractProjectViewPane.EP.findFirstSafe(project, pane -> ID.equals(pane.getId())));
  }
}

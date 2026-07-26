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

package org.apache.grails.intellij.plugin.structure.sync;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NlsContexts;
import com.intellij.openapi.util.NotNullLazyValue;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.structure.GrailsApplicationManager;

import java.util.Collection;
import java.util.function.Supplier;

public abstract class GrailsApplicationBackgroundTask extends Task.Backgroundable {
  protected final @NotNull NotNullLazyValue<Collection<GrailsApplication>> myApplicationsComputable;

  public GrailsApplicationBackgroundTask(@NotNull Project project, @NotNull @NlsContexts.ProgressTitle String title) {
    this(project, title, NotNullLazyValue.lazy(new GrailsApplicationsGetter(project)));
  }

  protected GrailsApplicationBackgroundTask(@NotNull Project project,
                                            @NotNull @NlsContexts.ProgressTitle String title,
                                            @NotNull NotNullLazyValue<Collection<GrailsApplication>> computable) {
    super(project, title, false);
    myApplicationsComputable = computable;
  }

  @Override
  public final void run(@NotNull ProgressIndicator indicator) {
    final Collection<GrailsApplication> applications = myApplicationsComputable.getValue();
    for (GrailsApplication application : applications) {
      indicator.checkCanceled();
      if (!application.isValid()) continue;
      indicator.setText(GrailsBundle.message("progress.indicator.text.0.in.1", getTitle(), application.getName()));
      run(application, indicator);
    }
  }

  protected abstract void run(@NotNull GrailsApplication application, @NotNull ProgressIndicator indicator);

  /**
   * Lazily gets Grails applications inside read action.
   */
  private static final class GrailsApplicationsGetter implements Supplier<Collection<GrailsApplication>> {
    private final @NotNull Project myProject;

    private GrailsApplicationsGetter(@NotNull Project project) {
      myProject = project;
    }

    @Override
    public @NotNull Collection<GrailsApplication> get() {
      return ReadAction.compute(() -> GrailsApplicationManager.getInstance(myProject).getApplications());
    }
  }
}

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

package org.jetbrains.plugins.grails.runner;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.groovy.mvc.ConsoleProcessDescriptor;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

public abstract class GrailsCommandExecutor {
  public static final ExtensionPointName<GrailsCommandExecutor> EP_NAME = new ExtensionPointName<>("org.intellij.grails.commandExecutor");

  @Contract("null -> null")
  public static @Nullable GrailsCommandExecutor getGrailsExecutor(@Nullable GrailsApplication grailsApplication) {
    if (grailsApplication == null) return null;

    for (GrailsCommandExecutor executor : EP_NAME.getExtensions()) {
      if (executor.isApplicable(grailsApplication)) {
        return executor;
      }
    }

    return null;
  }

  public abstract boolean isApplicable(@NotNull GrailsApplication grailsApplication);

  public abstract @Nullable RunProfileState getState(@NotNull GrailsRunConfiguration configuration,
                                                     @NotNull Executor executor,
                                                     @NotNull ExecutionEnvironment environment) throws ExecutionException;

  public abstract @Nullable ConsoleProcessDescriptor execute(@NotNull GrailsApplication application,
                                                             @NotNull MvcCommand command,
                                                             @Nullable Runnable onDone,
                                                             boolean close,
                                                             String... input) throws ExecutionException;
}

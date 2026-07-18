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
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.projectRoots.Sdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.sdk.GrailsSDK;
import org.jetbrains.plugins.groovy.mvc.MvcCommand;

public interface GrailsInstallationExecutor {

  boolean isApplicable(@NotNull GrailsSDK grailsSdk);

  @NotNull
  JavaParameters createJavaParameters(@NotNull Sdk sdk, @NotNull GrailsSDK grailsSdk, @NotNull MvcCommand command) throws ExecutionException;
}


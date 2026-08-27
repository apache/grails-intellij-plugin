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
import com.intellij.execution.configurations.JavaParameters;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.NlsContexts.TabTitle;
import com.intellij.openapi.util.Pair;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
import org.apache.grails.intellij.plugin.mvc.MvcCommand;

public interface GrailsRunConfigurationExtension<T> {

  @NotNull
  Key<T> getKey();

  default @Nullable SettingsEditor<GrailsRunConfiguration> createExtensionEditor() {
    return null;
  }

  default @Nullable Pair<@TabTitle String, SettingsEditor<GrailsRunConfiguration>> createSettingsEditor(@NotNull Project project) {
    return null;
  }

  @Nullable
  T readAdditionalConfiguration(@NotNull Element element);

  void writeAdditionalConfiguration(@NotNull T cfg, @NotNull Element element);

  @NotNull
  JavaParameters createJavaParameters(@NotNull GrailsApplication grailsApplication,
                                      @NotNull MvcCommand command,
                                      @Nullable T additionalConfiguration) throws ExecutionException;
}

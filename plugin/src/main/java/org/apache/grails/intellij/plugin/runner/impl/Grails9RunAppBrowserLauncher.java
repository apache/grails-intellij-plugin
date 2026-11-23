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

package org.apache.grails.intellij.plugin.runner.impl;

import com.intellij.execution.ExecutionListener;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.util.UserDataHolder;
import org.apache.grails.intellij.plugin.runner.util.GrailsExecutionUtils;
import org.jetbrains.annotations.NotNull;

/**
 * Opens the browser for a {@code run-app} of a project of Grails 9 that the Gradle support runs
 * ({@link Grails9RunApp}), once the application says where it runs, as the Grails run configuration does for the
 * Grails shell. The Gradle run carries the address in {@link Grails9RunApp#LAUNCH_BROWSER_URL}.
 */
public final class Grails9RunAppBrowserLauncher implements ExecutionListener {

  @Override
  public void processStarted(@NotNull String executorId, @NotNull ExecutionEnvironment env, @NotNull ProcessHandler handler) {
    if (!(env.getRunProfile() instanceof UserDataHolder profile)) return;
    String url = profile.getUserData(Grails9RunApp.LAUNCH_BROWSER_URL);
    if (url == null) return;
    handler.addProcessListener(GrailsExecutionUtils.getBrowserLaunchListener(handler, url.isEmpty() ? null : url));
  }
}

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

package org.apache.grails.intellij.plugin.service;

import com.intellij.openapi.components.Service;
import com.intellij.openapi.progress.BackgroundTaskQueue;
import com.intellij.openapi.progress.Task;
import com.intellij.openapi.project.Project;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GrailsBundle;

@Service(Service.Level.PROJECT)
public final class GrailsBackgroundService {
  private final BackgroundTaskQueue myQueue;

  public GrailsBackgroundService(Project project) {
    myQueue = new BackgroundTaskQueue(project, GrailsBundle.message("task.queue.title.grails.background.tasks"));
  }

  public void run(Task.Backgroundable task) {
    myQueue.run(task);
  }

  public static @NotNull GrailsBackgroundService getInstance(@NotNull Project project) {
    return project.getService(GrailsBackgroundService.class);
  }
}

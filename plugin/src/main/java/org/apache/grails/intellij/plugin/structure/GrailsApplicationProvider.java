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

package org.apache.grails.intellij.plugin.structure;

import com.intellij.openapi.extensions.ExtensionPointName;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class GrailsApplicationProvider {

  public static final ExtensionPointName<GrailsApplicationProvider> APPLICATION_PROVIDER =
    ExtensionPointName.create("org.intellij.grails.applicationProvider");

  /**
   * Implementations should check if some root corresponds to some Grails application.
   *
   * @param root application root, that is the parent folder of some grails-app.
   * @return instance of Grails application or {@code null}.
   */
  public abstract @Nullable GrailsApplication createApplication(@NotNull Project project, @NotNull VirtualFile root);

  public static @Nullable GrailsApplication createGrailsApplication(@NotNull Project project, @NotNull VirtualFile root) {
    for (GrailsApplicationProvider provider : APPLICATION_PROVIDER.getExtensions()) {
      final GrailsApplication application = provider.createApplication(project, root);
      if (application != null) return application;
    }
    return null;
  }
}

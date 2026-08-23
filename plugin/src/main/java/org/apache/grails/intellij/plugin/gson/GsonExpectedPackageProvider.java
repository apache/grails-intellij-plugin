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
package org.apache.grails.intellij.plugin.gson;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GsonConstants;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;
import org.jetbrains.plugins.groovy.lang.resolve.ExpectedPackageNameProvider;

// Public: instantiated reflectively from plugin.xml (expectedPackageNameProvider). It was Kotlin
// internal, which is public in bytecode, so package-private here would fail at runtime.
public final class GsonExpectedPackageProvider implements ExpectedPackageNameProvider {

  @Override
  public @Nullable String inferPackageName(@NotNull GroovyFile file) {
    // GSON views are scripts at the views root, so they belong to the default package.
    return file.getVirtualFile().getNameSequence().toString().endsWith(GsonConstants.FILE_SUFFIX) ? "" : null;
  }
}

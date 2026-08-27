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
package org.jetbrains.plugins.groovy;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.openapi.roots.ModuleRootModificationUtil;
import org.jetbrains.annotations.NotNull;

public interface TestLibrary {

    default void addTo(@NotNull Module module) {
        ModuleRootModificationUtil.updateModel(module, model -> addTo(module, model));
    }

    void addTo(@NotNull Module module, @NotNull ModifiableRootModel model);

    @NotNull
    default TestLibrary plus(@NotNull TestLibrary library) {
        return new CompoundTestLibrary(this, library);
    }
}

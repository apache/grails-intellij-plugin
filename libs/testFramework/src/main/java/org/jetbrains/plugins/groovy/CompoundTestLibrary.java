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

package org.jetbrains.plugins.groovy;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.roots.ModifiableRootModel;
import com.intellij.testFramework.IndexingTestUtil;
import org.jetbrains.annotations.NotNull;

public final class CompoundTestLibrary implements TestLibrary {
    public CompoundTestLibrary(TestLibrary... libraries) {
        assert libraries.length > 0;
        myLibraries = libraries;
    }

    @Override
    public void addTo(@NotNull Module module, @NotNull ModifiableRootModel model) {
        for (TestLibrary library : myLibraries) {
            library.addTo(module, model);
        }
        IndexingTestUtil.waitUntilIndexesAreReady(model.getProject());
    }

    private final TestLibrary[] myLibraries;
}

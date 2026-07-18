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
package org.jetbrains.plugins.gradle.tooling;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.gradle.tooling.annotation.TargetJavaVersion;
import org.jetbrains.plugins.gradle.tooling.jvm.GradleBrokenJvmSerialisationVersionRestriction;
import org.jetbrains.plugins.gradle.tooling.jvm.GradleToolingApi9VersionRestriction;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;

import java.util.ArrayList;
import java.util.List;

public class TargetJavaVersionWatcher extends TestWatcher {

    @Nullable
    private JavaVersionRestriction myRestriction;

    public TargetJavaVersionWatcher() {
        this(null);
    }

    public TargetJavaVersionWatcher(@Nullable JavaVersionRestriction restriction) {
        List<JavaVersionRestriction> effectiveRestrictions = new ArrayList<>();
        effectiveRestrictions.add(new GradleToolingApi9VersionRestriction());
        effectiveRestrictions.add(new GradleBrokenJvmSerialisationVersionRestriction());
        if (restriction != null) {
            effectiveRestrictions.add(restriction);
        }
        myRestriction = JavaVersionRestriction.compositeOf(effectiveRestrictions);
    }

    public @NotNull JavaVersionRestriction getRestriction() {
        return myRestriction != null ? myRestriction : JavaVersionRestriction.NO;
    }

    @Override
    protected void starting(@NotNull Description description) {
        TargetJavaVersion targetJavaVersion = description.getAnnotation(TargetJavaVersion.class);
        if (targetJavaVersion == null) {
            return;
        }
        myRestriction = JavaVersionRestriction.javaRestrictionOf(targetJavaVersion.value());
    }
}

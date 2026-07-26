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
package org.jetbrains.plugins.gradle.tooling.jvm

import com.intellij.gradle.toolingExtension.util.GradleVersionUtil
import com.intellij.util.lang.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.tooling.JavaVersionRestriction

class GradleToolingApi9VersionRestriction : JavaVersionRestriction {
    override fun isRestricted(gradleVersion: GradleVersion, source: JavaVersion): Boolean {
        return GradleVersionUtil.isGradleAtLeast(gradleVersion, "7.3") && source.feature < 17
    }
}
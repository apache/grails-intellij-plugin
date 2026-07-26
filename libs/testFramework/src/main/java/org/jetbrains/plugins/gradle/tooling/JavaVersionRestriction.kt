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
package org.jetbrains.plugins.gradle.tooling

import com.intellij.util.lang.JavaVersion
import org.gradle.util.GradleVersion
import org.jetbrains.plugins.gradle.tooling.jvm.GradleBrokenJvmSerialisationVersionRestriction
import org.jetbrains.plugins.gradle.tooling.jvm.GradleToolingApi9VersionRestriction
import org.jetbrains.plugins.gradle.tooling.util.JavaVersionMatcher.isVersionMatch

fun interface JavaVersionRestriction {

    fun isRestricted(gradleVersion: GradleVersion, source: JavaVersion): Boolean

    companion object {
        @JvmField
        val NO = JavaVersionRestriction { _, _ -> false }

        @JvmField
        val DEFAULT = compositeOf(listOf(
            GradleToolingApi9VersionRestriction(),
            GradleBrokenJvmSerialisationVersionRestriction()
        ))

        /**
         * @param targetVersionNotation the java version restriction in string form.
         * The notation variants can be found in the [org.jetbrains.plugins.gradle.tooling.annotation.TargetJavaVersion] documentation.
         */
        @JvmStatic
        fun javaRestrictionOf(targetVersionNotation: String): JavaVersionRestriction {
            return JavaVersionRestriction { _, source -> !isVersionMatch(source, targetVersionNotation) }
        }

        @JvmStatic
        fun compositeOf(restrictions: List<JavaVersionRestriction>): JavaVersionRestriction {
            return JavaVersionRestriction { gradleVersion, source ->
                restrictions.any { it.isRestricted(gradleVersion, source) }
            }
        }
    }
}
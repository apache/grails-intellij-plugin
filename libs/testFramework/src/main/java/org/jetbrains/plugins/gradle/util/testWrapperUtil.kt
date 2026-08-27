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
package org.jetbrains.plugins.gradle.util

import com.intellij.openapi.project.Project
import com.intellij.platform.eel.provider.getEelDescriptor
import com.intellij.util.io.delete
import org.gradle.StartParameter
import org.gradle.util.GradleVersion
import org.gradle.wrapper.PathAssembler
import org.jetbrains.plugins.gradle.service.execution.gradleUserHomeDir
import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipInputStream
import kotlin.io.path.ExperimentalPathApi
import kotlin.io.path.exists

@OptIn(ExperimentalPathApi::class)
internal fun validateGradleJar(gradleJar: Path) {
    if (!gradleJar.exists()) {
        return
    }
    try {
        Files.newInputStream(gradleJar).use {
            ZipInputStream(it).use {
                // sanity check
            }
        }
    }
    catch (e: Exception) {
        e.printStackTrace()
        println("Corrupted Gradle wrapper file will be removed: $gradleJar")
        gradleJar.delete()
    }
}

@Suppress("IO_FILE_USAGE")
internal fun getGradleDistributionJarPath(projectPath: Path): Path {
    val descriptor = projectPath.getEelDescriptor()
    // the file used only as a path-representation; no IO operations are expected
    return PathAssembler(gradleUserHomeDir(descriptor).toFile(), projectPath.toFile())
        .getDistribution(GradleUtil.getWrapperConfiguration(projectPath))
        .zipFile
        .toPath()
}

internal fun getGradleDistributionRoot(project: Project, version: GradleVersion): Path {
    val descriptor = project.getEelDescriptor()
    return getGradleDistributionRoot(gradleUserHomeDir(descriptor), version)
}

internal fun getLocalGradleDistributionRoot(version: GradleVersion): Path =
    getGradleDistributionRoot(StartParameter.DEFAULT_GRADLE_USER_HOME.toPath(), version)

internal fun getGradleDistributionRoot(gradleUserHome: Path, version: GradleVersion): Path =
    gradleUserHome.resolve("wrapper/dists/gradle-${version.version}-bin")
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
package com.intellij.platform.testFramework.eelJava

import org.jetbrains.annotations.ApiStatus
import java.nio.file.Path

@ApiStatus.Internal
object EelTestUtil {

    fun isEelRequired(): Boolean = !isLocalRun()

    fun isLocalRun(): Boolean = getFixtureEngine() == EelFixtureEngine.NONE

    fun getFileSystemMount(): Path {
        val mount = System.getenv("EEL_FIXTURE_MOUNT")
            ?: throw IllegalArgumentException("The EEL_FIXTURE_MOUNT environment variable is not specified")
        return Path.of(mount)
    }

    fun getFixtureEngine(): EelFixtureEngine {
        val engine = System.getenv("EEL_FIXTURE_ENGINE") ?: return EelFixtureEngine.NONE
        return EelFixtureEngine.valueOf(engine.uppercase())
    }

    fun getEelFixtureEngineJavaHome(): Path {
        val path = System.getenv("EEL_FIXTURE_ENGINE_JAVA_HOME")
            ?: throw IllegalArgumentException("The system environment variable EEL_FIXTURE_ENGINE_JAVA_HOME should be explicitly specified")
        return Path.of(path)
    }

    fun getTeamcityWslJdkDefinition(): Path? {
        return System.getenv("TEAMCITY_WSL_JDK_DEFINITION")?.let { Path.of(it) }
    }

    enum class EelFixtureEngine {
        NONE,
        DOCKER,
        WSL
    }

}
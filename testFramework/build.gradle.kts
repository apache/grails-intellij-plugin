import org.gradle.api.internal.tasks.JvmConstants
import org.jetbrains.intellij.platform.gradle.TestFrameworkType

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
fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform.module")
}

group = "org.apache.grails.intellij.testFramework"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

kotlin {
    // no toolchain on purpose: the JDK is pinned via .sdkmanrc for reproducible builds
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(properties("platformJavaVersion"))
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(properties("platformVersion"))
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("com.intellij.gradle") // split out of org.jetbrains.plugins.gradle in 2026.2
        bundledPlugin("org.jetbrains.plugins.gradle")

        // add to _production_ deps
        testFramework(
            type = TestFrameworkType.Plugin.Java,
            configurationName = JvmConstants.COMPILE_ONLY_CONFIGURATION_NAME
        )
        testFramework(
            type = TestFrameworkType.Platform,
            configurationName = JvmConstants.COMPILE_ONLY_CONFIGURATION_NAME
        )
        testFramework(
            type = TestFrameworkType.Plugin.Maven,
            configurationName = JvmConstants.COMPILE_ONLY_CONFIGURATION_NAME
        )
        testFramework(
            type = TestFrameworkType.Plugin.ExternalSystem,
            configurationName = JvmConstants.COMPILE_ONLY_CONFIGURATION_NAME
        )

    }
    compileOnly("junit:junit:4.13.2")
    compileOnly("org.assertj:assertj-core:4.0.0-M1")

    compileOnly(project(":"))
}

java.sourceSets["main"].java {
    srcDir("src")
}

tasks {
        withType<JavaCompile> {
        sourceCompatibility = properties("platformJavaVersion")
        targetCompatibility = properties("platformJavaVersion")
    }

}

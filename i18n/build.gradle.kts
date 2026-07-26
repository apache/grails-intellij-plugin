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
    id("org.apache.grails.intellij.build.reproducible")
    id("org.apache.grails.intellij.build.jacoco")
    id("org.apache.grails.intellij.build.vulnerability-scan")
    id("org.jetbrains.intellij.platform.module")
}

group = "org.apache.grails.intellij.i18n"

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(properties("platformVersion"))
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("com.intellij.java-i18n")
    }

    compileOnly(project(":"))
}

java.sourceSets["main"].java {
    srcDir("src")
}

java.sourceSets["main"].resources {
    srcDir("resources")
}

tasks {
        withType<JavaCompile> {
        sourceCompatibility = properties("platformJavaVersion")
        targetCompatibility = properties("platformJavaVersion")
    }

}

// 2026.2 resolves content-module descriptors from lib/modules/<module-name>.jar,
// so the composed jar must be named after the content module, not the project
tasks.named<org.jetbrains.intellij.platform.gradle.tasks.ComposedJarTask>("composedJar") {
    archiveBaseName = "intellij.groovy.grails.i18n"
}

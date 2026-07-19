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
    id("org.jetbrains.intellij.platform.module")
}

group = "org.apache.grails.intellij.compiler.patch"

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
    }

    compileOnly("org.grails:grails-core:1.2.0") {
        exclude(group = "org.codehaus.groovy") // groovy pinned explicitly below
    }
    // era-correct Groovy for the Grails 1.x/2.x patchers (superset of the APIs used);
    // must NOT be the IDE's bundled Groovy 5 — see compileClasspath exclusion below
    compileOnly("org.codehaus.groovy:groovy-all:2.4.21")
}

// this patch runs inside Grails 2.x builds, so it must compile against the era-correct
// Groovy (groovy-all 1.6.7 via grails-core above), not the IDE's bundled Groovy 5
configurations.compileClasspath {
    exclude(group = "bundledPlugin", module = "com.intellij.groovy.scripting")
}

java.sourceSets["main"].java {
    srcDir("src")
}

java.sourceSets["main"].resources {
    srcDir("resources")
}

tasks {
    // Set the JVM compatibility versions
    withType<JavaCompile> {
        sourceCompatibility = "8"
        targetCompatibility = "8"
        // IPGP 2.18+ force-sets options.release to the platform's Java version (25);
        // this module must stay Java 8 bytecode (it runs inside user Grails/JPS builds)
        options.release = 8
    }

}


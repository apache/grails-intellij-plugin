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
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.11.0"
    id("org.nosphere.apache.rat") version "0.8.1"
}

group = "org.apache.grails.intellij"
version = properties("pluginVersion")

repositories {
    mavenCentral()
    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        intellijIdea(properties("platformVersion"))

        bundledPlugin("com.intellij.javaee")
        bundledPlugin("com.intellij.persistence")
        bundledPlugin("com.intellij.javaee.jpa")
        bundledPlugin("com.intellij.jsp")
        bundledPlugin("com.intellij.javaee.web")
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("com.intellij.database")
        bundledPlugin("com.intellij.spring")
        bundledPlugin("org.jetbrains.plugins.gradle")
        bundledPlugin("com.intellij.modules.ultimate")
        bundledPlugin("com.intellij.microservices.jvm")
        bundledPlugin("JavaScript")
        bundledPlugin("com.intellij.css")


        // plugin modules
        pluginModule(project(":copyright"))
        pluginModule(project(":coverage"))
        pluginModule(project(":hibernate"))
        pluginModule(project(":i18n"))
        pluginModule(project(":langInjection"))
        pluginModule(project(":maven"))


        // additional plugins required for testing
        testBundledPlugin("com.intellij.hibernate")
        testBundledPlugin("HtmlTools")
        testBundledPlugin("org.jetbrains.idea.maven")

        testFramework(TestFrameworkType.Plugin.ExternalSystem)
        testFramework(TestFrameworkType.Plugin.Java)
        testFramework(TestFrameworkType.Plugin.Maven)
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:4.0.0-M1")
    testImplementation(project(":testFramework"))
    testCompileOnly(project(":i18n"))

    implementation(project(":gradle-tooling"))
    implementation(project(":grails-rt"))

    // JPS/build
    runtimeOnly(project(":grails-compiler-patch"))
    runtimeOnly(project(":jps-plugin"))
}

kotlin {
    jvmToolchain(21)
}

java.sourceSets["main"].java {
    srcDir("src")
    srcDir("gen")
}

java.sourceSets["main"].resources {
    srcDir("resources")
    srcDir("compatibilityResources")
}

// special handling for /standardDsls/ packaging, see PrepareSandboxTask below
sourceSets {
    main {
        resources {
            exclude("standardDsls/")
        }
    }
}

java.sourceSets["test"].java {
    srcDir("test")
}

intellijPlatform {
    pluginConfiguration {
        ideaVersion {
            sinceBuild = properties("pluginSinceBuild")
        }

        changeNotes = ""
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = properties("platformJavaVersion")
        targetCompatibility = properties("platformJavaVersion")
    }

    test {
        systemProperty("idea.home.path", properties("test.idea.home.path"))
    }

    // special handling for /standardDsls/ packaging
    withType<PrepareSandboxTask> {
        from(layout.projectDirectory.dir("resources/standardDsls")) {
            into(pluginName.map { "$it/lib/standardDsls/"})
        }
    }
}

// ASF license audit (Apache RAT) — see MIGRATION-PLAN.md Phase 3.
// Every exclude must carry a justification; no blanket excludes to make the audit pass.
tasks.rat {
    excludes.addAll(
        listOf(
            // planning documents, removed before the first release
            "MIGRATION-PLAN.md",
            "IMPROVEMENT-PLAN.md",
            // documentation
            "**/README.md",
            // git configuration isn't code
            "**/.gitignore",
            "**/.gitattributes",
            // gradle wrapper files, excluded from the source distro
            "gradlew*",
            "gradle/wrapper/**",
            "**/.gradle/**",
            // github/ASF infra configuration, not shipped in the source distro
            ".github/**",
            ".asf.yaml",
            // IDE-generated files
            ".idea/**",
            "**/*.iml",
            "out/**",
            // IntelliJ Platform Gradle Plugin local cache (generated Ivy descriptors, git-ignored)
            ".intellijPlatform/**",
            // generated code (JFlex lexers; headers are injected by the generator config)
            "gen/**",
            // test fixtures: content *is* the test input (mock Grails projects/JARs);
            // adding headers would break parser/position-sensitive tests
            "testdata/**",
            "**/testdata/**",
            // file templates that materialize inside end-user projects
            "**/resources/fileTemplates/**",
            // intention/inspection description snippets and before/after templates
            // rendered verbatim in the IDE settings UI
            "**/intentionDescriptions/**",
            "**/inspectionDescriptions/**",
            // html files are IDE description snippets / template previews only
            "**/*.html",
            // GUI-designer .form files are machine-edited XML
            "**/*.form",
            // images
            "**/*.png", "**/*.svg", "**/*.ico", "**/*.jpg", "**/*.gif",
            // JAR manifest format has no comment syntax
            "grails-rt/resources/META-INF/MANIFEST.MF",
        ) + subprojects.map {
            // per-subproject build directories
            it.layout.buildDirectory.get().asFile.relativeTo(rootProject.projectDir).path + "/**"
        } + listOf("build/**")
    )
    // never cache license audits
    outputs.upToDateWhen { false }
}

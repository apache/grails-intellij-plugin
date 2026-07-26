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
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.intellij.platform.gradle.TestFrameworkType
import org.jetbrains.intellij.platform.gradle.tasks.PrepareSandboxTask

fun properties(key: String) = project.findProperty(key).toString()

plugins {
    id("java")
    id("org.jetbrains.kotlin.jvm") version "2.3.0"
    id("org.jetbrains.intellij.platform") version "2.18.1"
    id("org.nosphere.apache.rat") version "0.8.1"
    // build-hygiene conventions (build-logic included build)
    id("jacoco-report-aggregation")
    id("org.apache.grails.intellij.build.reproducible")
    id("org.apache.grails.intellij.build.jacoco")
    id("org.apache.grails.intellij.build.vulnerability-scan")
}

group = "org.apache.grails.intellij"
// project version comes from gradle.properties `version` (override with -Pversion=)

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
        bundledPlugin("com.intellij.javaee.el") // no longer transitive in 2026.2
        bundledPlugin("com.intellij.persistence")
        bundledPlugin("com.intellij.javaee.jpa")
        bundledPlugin("com.intellij.jsp")
        bundledPlugin("com.intellij.javaee.web")
        bundledPlugin("org.intellij.groovy")
        bundledPlugin("com.intellij.database")
        bundledPlugin("com.intellij.spring")
        bundledPlugin("com.intellij.gradle") // split out of org.jetbrains.plugins.gradle in 2026.2
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
        testBundledPlugin("com.intellij.copyright") // GspCopyrightUpdaterTest

        testFramework(TestFrameworkType.Plugin.ExternalSystem)
        testFramework(TestFrameworkType.Plugin.Java)
        testFramework(TestFrameworkType.Plugin.Maven)
        testFramework(TestFrameworkType.Platform)
    }

    testImplementation("junit:junit:4.13.2")
    testImplementation("org.assertj:assertj-core:4.0.0-M1")
    testImplementation(project(":testFramework"))
    testCompileOnly(project(":i18n"))
    testCompileOnly(project(":copyright"))

    implementation(project(":gradle-tooling"))
    implementation(project(":grails-rt"))

    // JPS/build
    runtimeOnly(project(":grails-compiler-patch"))
    runtimeOnly(project(":jps-plugin"))

    // Aggregate JaCoCo coverage from every module into the root testCodeCoverageReport.
    jacocoAggregation(project(":copyright"))
    jacocoAggregation(project(":coverage"))
    jacocoAggregation(project(":gradle-tooling"))
    jacocoAggregation(project(":grails-compiler-patch"))
    jacocoAggregation(project(":grails-rt"))
    jacocoAggregation(project(":hibernate"))
    jacocoAggregation(project(":i18n"))
    jacocoAggregation(project(":jps-plugin"))
    jacocoAggregation(project(":langInjection"))
    jacocoAggregation(project(":maven"))
    jacocoAggregation(project(":testFramework"))
}

// jacoco-report-aggregation auto-registers testCodeCoverageReport from the default `test`
// suite, producing build/reports/jacoco/testCodeCoverageReport/... ; ensure the XML variant
// is emitted for Codecov upload.
tasks.named<JacocoReport>("testCodeCoverageReport") {
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

kotlin {
    // no toolchain on purpose: the JDK is pinned via .sdkmanrc for reproducible builds
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.fromTarget(properties("platformJavaVersion"))
    }
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

    pluginVerification {
        // the legacy plugin id org.intellij.grails is grandfathered on Marketplace and
        // permanent (see MIGRATION-PLAN.md decision 2); mute the new-plugin naming check
        freeArgs = listOf("-mute", "TemplateWordInPluginId")
        // gate on real incompatibilities only; the inherited internal/deprecated API
        // usages (47/67) are a tracked cleanup item, not a release blocker
        failureLevel = listOf(
            org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.COMPATIBILITY_PROBLEMS,
            org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.MISSING_DEPENDENCIES,
            org.jetbrains.intellij.platform.gradle.tasks.VerifyPluginTask.FailureLevel.INVALID_PLUGIN,
        )
        ides {
            recommended()
        }
    }

    // JetBrains Marketplace code signing / publishing; secrets provided by the release workflow
    signing {
        certificateChain = providers.environmentVariable("CERTIFICATE_CHAIN")
        privateKey = providers.environmentVariable("PRIVATE_KEY")
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }
}

tasks {
    withType<JavaCompile> {
        sourceCompatibility = properties("platformJavaVersion")
        targetCompatibility = properties("platformJavaVersion")
    }

    test {
        // local override for tests that need IDE sources; unset/nonexistent on CI
        val ideaHome = project.findProperty("test.idea.home.path")?.toString()
        if (ideaHome != null && file(ideaHome).exists()) {
            systemProperty("idea.home.path", ideaHome)
        }
    }

    // ASF policy: ship LICENSE and NOTICE inside the plugin jar
    processResources {
        from(layout.projectDirectory.file("LICENSE")) { into("META-INF") }
        from(layout.projectDirectory.file("NOTICE")) { into("META-INF") }
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
            // build-logic included build: generated output and local caches (sources are headered)
            "build-logic/build/**",
            "build-logic/.gradle/**",
            // reproducible-build test output (git-ignored, regenerated by etc/bin/test-reproducible-build.sh)
            "etc/bin/results/**",
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
            // build outputs are generated and never shipped in the source distro. Listed as globs
            // rather than derived from subprojects so directories Gradle does not enumerate as
            // subprojects are covered too: included builds, and leftover build dirs from modules no
            // longer in settings.gradle.kts. Anchored to the root and one level down instead of
            // "**/build/**" so a source package named "build" would still be audited.
            "build/**",
            "*/build/**",
        )
    )
    // never cache license audits
    outputs.upToDateWhen { false }
}



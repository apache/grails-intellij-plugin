<!--
SPDX-License-Identifier: Apache-2.0

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Agent Guide for grails-intellij-plugin

> **IMPORTANT**: This is the IntelliJ IDEA plugin for Apache Grails (GSP language support,
> Grails project structure and navigation, run configurations, taglib/domain-class
> support), NOT the Grails framework itself and NOT a Grails application.

## Quick Reference

```bash
# Compile and run all tests (~3.5 min, ~970 tests)
./gradlew test

# Single test class / single test method
./gradlew test --tests "org.apache.grails.intellij.plugin.SomeTest"
./gradlew test --tests "org.apache.grails.intellij.plugin.SomeTest.testFeature"

# Build the plugin ZIP (written to build/distributions/)
./gradlew buildPlugin

# Full check / Plugin Verifier / license audit / sandbox IDE
./gradlew check
./gradlew verifyPlugin
./gradlew rat
./gradlew runIde
```

## Critical Rules

1. **Never trust a test run without checking for compile errors first.** If compilation
   fails during `./gradlew test`, Gradle silently runs the **stale previous bytecode**.
   Always grep the output for `error:` before believing pass/fail results or probe output.
2. **Collect failure lists from a full run only.** A `--tests`-filtered run **wipes**
   `plugin/build/test-results/test/`, destroying the results of the previous full run.
3. **Every code change must include tests.** Review the existing tests for the affected
   area before making changes, and keep every test that covers a modified class in sync
   with the new behavior. Run all affected tests and ensure they pass before committing.
4. **Apache license header required** on all new source files (enforced by `./gradlew rat`).
   Every RAT exclude in the `rat` convention plugin must carry an inline justification.
   - **New files use the ASF header** (verbatim from the `HEADER` file: *"Licensed to the
     Apache Software Foundation (ASF) under one or more contributor license agreements…"*),
     rendered as a `/* … */` block for Java/Kotlin or `<!-- … -->` for XML. **Do NOT** copy
     the `Copyright 2000-2026 JetBrains s.r.o. and contributors` header — that appears only on
     files inherited through the migration; new work is ASF-owned.
5. **Test-fixture JDK conventions (2026.2+)** — Mock JDK 1.7 is no longer shipped:
   - **Light fixtures**: `GrailsTestCase` pins `DefaultLightProjectDescriptor(IdeaTestUtil::getMockJdk11)`
     via `getTestJdk()`. Override `getTestJdk()` to a real JDK only when the test needs
     Swing/AWT classes.
   - **Heavy fixtures** (`JavaModuleFixtureBuilder`): use
     `moduleBuilder.addJdk(System.getProperty("java.home"))`. `IdeaTestUtil.getMockJdk11()`
     NPEs in `tuneFixture` because the test application doesn't exist yet.
   - Avoid `RepositoryTestLibrary`-backed descriptors (`GroovyProjectDescriptors.GROOVY_*`)
     — they fail light-project init in the plugin-dev SDK ("Cannot find IntelliJ IDEA
     project files"). Prefer `GroovyProjectDescriptors.MOCK_JDK_11` unless the test truly
     needs a Groovy jar on the classpath.
6. **Don't add license headers to `testdata/`** — the content *is* the test input;
   headers break parser/position-sensitive tests.
7. **No wildcard imports** — use explicit imports, matching the existing sources.
8. **JDK is pinned via `.sdkmanrc`** (Java 25, Gradle 9.3.1) — no Gradle toolchain on
   purpose, for reproducible builds. Run `sdk env` if the build complains about the JDK.
9. **Remove debug probes before committing** (see Debugging below).
10. **Retry transient commit failures.** Git commits can fail with
    `1Password: failed to fill whole buffer` (signing) — just retry.

## Technology Stack

| Component | Version |
|-----------|---------|
| IntelliJ Platform | 2026.2 Ultimate (`sinceBuild` 262) |
| JDK (build) | 25 (pinned in `.sdkmanrc`) |
| JDK (`grails-rt`, `grails-compiler-patch`, `jps-plugin`) | targets Java 8/11 |
| Gradle | 9.3.1 (wrapper) |
| IntelliJ Platform Gradle Plugin | 2.x |
| Kotlin | 2.3.x (stdlib not bundled) |
| Tests | JUnit 4 + AssertJ + IntelliJ test framework (light/heavy fixtures) |

## Project Structure

A composed build: `build-logic` is an included build holding every convention plugin, and the
subprojects sit in tier directories. Project names carry the subpath, so
`pluginModules/hibernate` is the Gradle project `:pluginModules-hibernate`. The root project
is a pure aggregator — it owns only RAT and coverage aggregation, no sources.

| Path | Gradle project | Description |
|------|----------------|-------------|
| `plugin/` | `:plugin` | Main plugin: GSP language, Grails project support, run configs |
| `pluginModules/{copyright,coverage,hibernate,i18n,langInjection,maven}/` | `:pluginModules-*` | Optional IntelliJ content modules (`pluginModule` deps) |
| `libs/gradle-tooling/` | `:libs-gradle-tooling` | Gradle tooling API model builders |
| `libs/grails-rt/` | `:libs-grails-rt` | Runtime injected into user apps (Java 8) |
| `libs/testFramework/` | `:libs-testFramework` | Shared test infrastructure (`GrailsTestCase`, `GroovyProjectDescriptors`, `TestLibrary`) |
| `compilers/{grails-compiler-patch,jps-plugin}/` | `:compilers-*` | JPS build integration |
| `build-logic/` | included build | Convention plugins, ids `org.apache.grails.intellij.build.*` |

> **`.gitignore` trap.** `build-logic`'s helper classes live in package
> `org.apache.grails.intellij.build`, i.e. a directory literally named `build`. A `**/build`
> ignore pattern silently swallows it, so those sources never get committed and every local
> build keeps working while a fresh checkout fails to compile the script plugins. The ignore
> patterns are therefore depth-anchored (`/build/`, `/*/build/`, `/*/*/build/`). After adding a
> file under `build-logic/src`, confirm it is not ignored:
> `git status --porcelain --ignored=matching | grep src/` should print nothing.

`plugin/` uses the standard source layout: `src/main/java` (Java, plus the few remaining
Kotlin files), `src/main/gen` for generated JFlex lexers, `src/main/resources`, and
`src/test/java`. `plugin/testdata/` sits next to the tests because Gradle sets
`Test.workingDir` to the project directory, which is how `GrailsTestUtil.getTestRootPath`
resolves it.

Special packaging: `plugin/standardDsls/` sits outside the resource roots and is copied to
`<plugin>/lib/standardDsls/` as loose files by a `PrepareSandboxTask` customization in the
`intellij-plugin` convention plugin.

## Running & Debugging Tests

- Full suite: `./gradlew test` — ~4 min, 987 tests across 186 classes.
- Failure details live in `plugin/build/test-results/test/TEST-<fqcn>.xml`; the `<system-out>`
  CDATA holds logged output. The giant module-list line and
  `InstanceNotOverridable`/SLF4J warnings are noise — ignore them.
- **Probe technique** that works well here: add `System.out.println("### TAG ...")`
  probes in `src/`, run one test, grep the result XML for `### TAG`. Remove probes
  before committing.
- Some tests can use IDE sources via `test.idea.home.path` in `gradle.properties`
  (points at a local intellij-community checkout; unset/nonexistent on CI is fine).
- Base classes: extend `GrailsTestCase` (in `testFramework`) for light-fixture tests;
  it handles the project descriptor and JDK. See Critical Rule 5 before touching
  fixture/JDK setup.

## Build Notes

- Gradle configuration cache and build cache are on; daemon runs with `-Xmx4g`
  (`buildPlugin` thrashes below that).
- Bundled-plugin dependencies are sensitive to platform version splits — e.g.
  `com.intellij.javaee.el` is no longer transitive and `com.intellij.gradle` was split
  out of `org.jetbrains.plugins.gradle` in 2026.2. When bumping `platformVersion`,
  expect to adjust the `bundledPlugin(...)` list in `plugin/build.gradle` (and in the
  `pluginModules/*/build.gradle` that declares the affected plugin).
- Plugin Verifier gates on real incompatibilities only (`COMPATIBILITY_PROBLEMS`,
  `MISSING_DEPENDENCIES`, `INVALID_PLUGIN`); the inherited internal/deprecated API
  usages are a tracked cleanup item, not a release blocker.
- The legacy plugin id `org.intellij.grails` is grandfathered on Marketplace and
  permanent (see `MIGRATION-PLAN.md`); the `TemplateWordInPluginId` check is muted
  deliberately.

## Pull Request Guidelines

1. **Fork & branch** from `main`.
2. **Run the full suite** before submitting: `./gradlew test` (see Critical Rules 1–2).
3. **Run the license audit**: `./gradlew rat`.
4. **Verify test coverage**: any touched class must be covered by tests, and all
   affected tests must pass.
5. **Squash commits** into a single meaningful commit message.
6. **Reference issues** in the PR description (e.g., "Fixes #123").

## Common Issues

| Problem | Solution |
|---------|----------|
| Test results look wrong / probes silent | Grep build output for `error:` — you may be running stale bytecode |
| Lost the failure list | Re-run the **full** `./gradlew test`; filtered runs wipe `plugin/build/test-results/test/` |
| `getMockJdk11()` NPE in `tuneFixture` | Heavy fixture — use `moduleBuilder.addJdk(System.getProperty("java.home"))` |
| "Cannot find IntelliJ IDEA project files" in light tests | Descriptor uses `RepositoryTestLibrary`; switch to `GroovyProjectDescriptors.MOCK_JDK_11` |
| Test needs Swing/AWT classes | Override `getTestJdk()` in the test to return a real JDK |
| Commit fails with `1Password: failed to fill whole buffer` | Transient signing hiccup — retry the commit |
| Wrong JDK / build fails to configure | `sdk env` (JDK pinned in `.sdkmanrc`, no toolchain) |
| RAT failure on a new file | Add the Apache license header; excludes need a justification |

## Resources

- **Grails**: https://grails.apache.org/
- **IntelliJ Platform SDK Docs**: https://plugins.jetbrains.com/docs/intellij/
- **IntelliJ Platform Gradle Plugin**: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
- **Issues**: https://github.com/apache/grails-intellij-plugin/issues
- **Mailing lists**: https://grails.apache.org/community/#mailing-lists

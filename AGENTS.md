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
# Compile and run all tests (~5 min)
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

# GitHub Actions policy: every `uses:` must be on the ASF approved list (needs network)
./gradlew validateActions

# Plugin Verifier against specific builds (e.g. the next EAP) instead of the recommended set
./gradlew verifyPlugin -PpluginVerifierIdes=IU-263.3889.65

# Community Edition: verifier, test suite and sandbox IDE on IdeaIC (downloads ~1GB once)
./gradlew verifyPlugin -PplatformEdition=IC
./gradlew :plugin:testIdeCe -PplatformEdition=IC
./gradlew :plugin:runIdeCe -PplatformEdition=IC
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
   - **Every file uses the ASF header** (verbatim from the `HEADER` file: *"Licensed to the
     Apache Software Foundation (ASF) under one or more contributor license agreements…"*),
     rendered as a `/* … */` block for Java/Kotlin/Gradle/Groovy, `#` for properties and
     `META-INF/services` files, or `<!-- … -->` for XML. **No copyright notice belongs in a
     header** — attribution lives in `NOTICE`. The `Copyright 2000-2026 JetBrains s.r.o. and
     contributors` form the codebase was imported with is gone as of the software grant and
     must not reappear.
   - `./gradlew rat` is the ongoing Apache 2.0 license check and runs in the RAT workflow.
     The one-time header migration is complete; use `HEADER` for new files.
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
8. **JDK is pinned via `.sdkmanrc`** (Java 25, Gradle 9.8.0) — no Gradle toolchain on
   purpose, for reproducible builds. Run `sdk env` if the build complains about the JDK.
   The `gradle=` pin must match the wrapper's `distributionUrl` (the source distribution
   bootstraps its wrapper from it); CI fails when they differ, so a wrapper bump also
   updates `.sdkmanrc`, `INSTALL`, `README.md` and this file.
9. **Remove debug probes before committing** (see Debugging below).
10. **Retry transient commit failures.** Git commits can fail with
    `1Password: failed to fill whole buffer` (signing) — just retry.
11. **The main jar must stay Community-loadable.** `plugin/src/main` compiles against the
    Community Edition API only; anything that needs an Ultimate plugin (Spring, Java EE,
    Database Tools, Hibernate/JPA) goes into a content module under `pluginModules/` whose
    descriptor names every plugin *and* content module it uses (a plugin id alone does not
    expose that plugin's content modules). The test suite runs on a flat classpath and the
    Ultimate verifier follows content-module dependencies, so neither catches a missing module
    dependency; `./gradlew verifyPlugin -PplatformEdition=IC` and `runIde` do. Tag tests that
    need Ultimate plugins with `@Category(UltimateOnlyTest.class)`. See `CE-SUPPORT.md`.
12. **Third-party GitHub Actions must be ASF-approved.** Anything outside `actions/*`, `github/*`
    and `apache/*` must be pinned to a full SHA listed in
    [apache/infrastructure-actions](https://github.com/apache/infrastructure-actions), with a
    trailing `# vX.Y.Z` comment. `./gradlew validateActions` checks this; the
    `validate-actions.yml` workflow runs it on workflow changes and weekly.

## Technology Stack

| Component | Version |
|-----------|---------|
| IntelliJ Platform | 2026.2.2 Ultimate (`sinceBuild` 262.10315.125) |
| JDK (build) | 25 (pinned in `.sdkmanrc`) |
| JDK (`grails-rt`, `grails-compiler-patch`, `jps-plugin`) | targets Java 8/11 |
| Gradle | 9.8.0 (wrapper) |
| IntelliJ Platform Gradle Plugin | 2.x |
| Kotlin | 2.4.x (stdlib not bundled) |
| Tests | JUnit 4 + AssertJ + IntelliJ test framework (light/heavy fixtures) |

## Project Structure

A composed build: `build-logic` is an included build holding every convention plugin, and the
subprojects sit in tier directories. Project names carry the subpath, so
`pluginModules/hibernate` is the Gradle project `:pluginModules-hibernate`. The root project
is a pure aggregator — it owns only RAT and coverage aggregation, no sources.

| Path | Gradle project | Description |
|------|----------------|-------------|
| `plugin/` | `:plugin` | Main plugin: GSP language, Grails project support, run configs. Compiles against the Community Edition API only. |
| `pluginModules/{copyright,coverage,database,hibernate,i18n,javaee,jsp,langInjection,maven,spring}/` | `:pluginModules-*` | Optional IntelliJ content modules (`pluginModule` deps). `spring`, `javaee`, `database` and `hibernate` hold the Ultimate-only integrations and are skipped on Community Edition |
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

## Project view gotchas

`plugin/src/main/java/.../projectView/` builds the Grails pane's tree. Label tests need to cover
both ordinary directories and module content roots, which the platform presents differently.

**Module content-root labels use coloured fragments; ordinary directories use `presentableText`.**
In platform 262.10315.125, `PsiDirectoryNode.updateImpl` adds coloured fragments only when
`ProjectRootsUtil.isModuleContentRoot` is true *and* the file resolves to at least one module; a content
root that resolves to none falls through to the `presentableText` path too. For those directories it calls
`setPresentableText` with the name from `ProjectViewDirectoryHelper.getNodeName`, which can be qualified
(`grails-app.i18n`).
Gradle's per-source-set modules make `src/test` a module content root, so its label can be
`test [app.test]` in fragments. The renderer prefers a non-empty fragment list over `presentableText`;
setting only the latter to `Tests:unit` leaves the content-root label visible.
`GrailsPsiDirectoryNode.postprocess` replaces any fragments and sets `presentableText`, keeping custom
titles consistent for both kinds of directory. `postprocess` itself is a choice rather than a requirement:
the platform has written the label by the time either hook's body runs, because `super.updateImpl` is the
first statement of the override, so clearing the fragments in `updateImpl` would work equally well. Do not
treat `updateImpl` as too early: `super.updateImpl` writes the label before the rest of the override runs.

**Tests must include `postprocess` and inspect the fragments when present.** Run `update()` then
read `getPresentation()` — or call `updateImpl` and then `postprocess` — to see the final custom title.
Asserting only `presentableText` misses a stale fragment list that the renderer would prefer.
A test that passes both with and without a labelling fix is not covering it: verify by reverting
the fix and watching the test fail.
Note that `postprocess` is `protected`, so a test outside `…projectView.nodes` must go through
`update()`; `GrailsNodeProviderTestSupport.rendered(node)` does that and is the helper to reach for.

**Light fixtures can reproduce content-root labels when the directory is registered accordingly.**
Use `ModuleRootModificationUtil.updateModel(module, model -> model.addContentEntry(dir))` to make
the directory a module content root. Assert that `updateImpl` produces platform fragments before
`postprocess`, then that the custom title replaces them. An ordinary `grails-app/i18n` directory
has no platform fragments to replace and does not reproduce the test-root bug.
`GrailsPsiDirectoryNodeTest` covers both cases, including preservation of fragments on untitled nodes.
Related: the Grails pane builds children in a background thread (`isToBuildChildrenInBackground` in
`GrailsProjectViewPane`), so nodes are built more than once and a node's identity hash changes
between builds — do not read that as a bug.

`GrailsViewItems.isHiddenFromOtherSources` is the single definition of "hidden from Other sources",
consulted by both the `assets` child filter and `OtherGrailsAppSourcesNode.contains()`. Two
independent implementations of that rule is what caused the nested-vendor-asset regression in
PR 432: the filter hid `assets/vendor/jquery-ui/images/` by name while `contains()` still claimed
files under it, so *Reveal in Project View* dead-ended.

## Running & Debugging Tests

- Full suite: `./gradlew test` — ~5 min.
- Failure details live in `plugin/build/test-results/test/TEST-<fqcn>.xml`; the `<system-out>`
  CDATA holds logged output. The giant module-list line and
  `InstanceNotOverridable`/SLF4J warnings are noise — ignore them.
- **Probe technique** that works well here: add `System.out.println("### TAG ...")`
  probes in `src/`, run one test, grep the result XML for `### TAG`. Remove probes
  before committing.
- Some tests can use IDE sources via `-Ptest.idea.home.path=<path>`, passed on the command
  line (points at a local intellij-community checkout). It is not set in the committed
  `gradle.properties`, and is silently ignored when the path does not exist.
- Base classes: extend `GrailsTestCase` (in `testFramework`) for light-fixture tests;
  it handles the project descriptor and JDK. See Critical Rule 5 before touching
  fixture/JDK setup.

## Build Notes

- Gradle configuration cache and build cache are on; daemon runs with `-Xmx4g`
  (`buildPlugin` thrashes below that).
- Bundled-plugin dependencies are sensitive to platform version splits — e.g.
  `com.intellij.javaee.el` is no longer transitive and `com.intellij.gradle` was split
  out of `org.jetbrains.plugins.gradle` in 2026.2. The JSP implementation (`com.intellij.jsp`)
  is a pinned Marketplace dependency rather than a bundled plugin in 2026.2.2.
  When bumping `platformVersion`,
  expect to adjust the `bundledPlugin(...)` list in `plugin/build.gradle` (and in the
  `pluginModules/*/build.gradle` that declares the affected plugin).
- Plugin Verifier gates on real incompatibilities only (`COMPATIBILITY_PROBLEMS`,
  `MISSING_DEPENDENCIES`, `INVALID_PLUGIN`); the remaining internal/deprecated API
  usages are a tracked cleanup item, not a release blocker. The per-usage lists are in
  `plugin/build/reports/pluginVerifier/<IDE>/plugins/org.intellij.grails/<version>/*.txt`.
  `-PpluginVerifierIdes=IU-<build>[,IU-<build>]` verifies against exactly those builds, which
  is how a verdict against an EAP is reproduced before it becomes the recommended release.
- The legacy plugin id `org.intellij.grails` is grandfathered on Marketplace and
  permanent for the existing listing (see `IMPROVEMENT-PLAN.md`); the `TemplateWordInPluginId` check is muted
  deliberately.
- **Coverage needs two non-default JaCoCo settings** (both in the `jacoco` and
  `coverage-aggregation` convention plugins): the tests load the plugin from the sandbox jars,
  which are built from the *instrumented* classes, so reports must analyse
  `build/instrumented/instrumentCode` rather than `build/classes`; and IntelliJ's plugin class
  loaders define classes without a code-source location, so the agent needs
  `includeNoLocationClasses = true` or the exec file contains only Gradle worker classes. Either
  one missing shows up as 0% for every class, not as an error.

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
| A feature "missing" after switching plugin builds | Rebuild before judging — hit the Gradle refresh icon (or `./gradlew buildPlugin`) so the sandbox picks up the new classes. A stale build can make working code look broken, and the Grails project view pane is the usual tell because it is only added once an application is detected |
| A project-view node shows the wrong label, or a filter change has no visible effect | Read "Project view gotchas" above before changing anything in `projectView/` — module content-root labels use coloured fragments that take precedence over `presentableText`; light-fixture coverage must register a content entry to exercise that case |


## Resources

- **Grails**: https://grails.apache.org/
- **IntelliJ Platform SDK Docs**: https://plugins.jetbrains.com/docs/intellij/
- **IntelliJ Platform Gradle Plugin**: https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html
- **Issues**: https://github.com/apache/grails-intellij-plugin/issues
- **Mailing lists**: https://grails.apache.org/community/#mailing-lists

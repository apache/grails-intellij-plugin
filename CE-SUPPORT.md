<!--
Licensed to the Apache Software Foundation (ASF) under one or more
contributor license agreements.  See the NOTICE file distributed with
this work for additional information regarding copyright ownership.
The ASF licenses this file to You under the Apache License, Version 2.0
(the "License"); you may not use this file except in compliance with
the License.  You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
-->

# Community Edition Support

> Status of the Apache Grails IntelliJ plugin on IntelliJ IDEA Community Edition (CE)
> since 262.0.1 (IDEA 2026.2+).

## What works in Community Edition

All of the core GSP language features work on CE without requiring IntelliJ Ultimate:

- **GSP parsing and highlighting** — GSP tag/attribute recognition, syntax highlighting,
  XML structure highlighting, error highlighting
- **GSP formatting** — code style for GSP/HTML/Groovy embedded sections (excluding
  JavaScript blocks, which fall back to raw text formatting)
- **Taglibs** — Grails tag library resolution, completion, named-argument references,
  tag namespace priority, and tag attribute name completion
- **Domain classes** — navigation to/from domain classes, GORM method resolution,
  `GrailsTaglibDescriptor` for all built-in tags (`g:link`, `g:render`, etc.)
- **Controllers** — navigation, controller class recognition
- **Views** — view resolution, template navigation
- **Services** — service class recognition
- **Project structure** — Grails project detection, folder structure (grails-app/views,
  grails-app/controllers, grails-app/domain, grails-app/services), with dedicated
  top-level nodes for `grails-app/i18n` (**Translations**) and the `grails-app/assets`
  subfolders (**Stylesheets**, **Images**, **JavaScripts**)
- **Run configurations** — Grails run configurations for application startup
- **Groovy injections** — Groovy URL/expr attribute completion and highlighting
- **Spring bean DSL** — `beans {}` definition resolution in
  `grails-app/conf/spring/resources.groovy` and in `doWithSpring` closures of Grails plugin
  classes (bean names, `ref()`, bean properties). The DSL contributor
  (`GrailsResourcesGroovyMemberContributor`) is independent of the Spring plugin and loads
  unconditionally since 262.1.0; bean-name "usage highlighting" there still needs the Spring
  plugin and stays Ultimate-only.
- **HTML tools** — HTML attribute completion and analysis (basic, not JSP-level)
- **Code navigation** — go-to-definition for Grails tags, domain methods, controllers

## What does not work in Community Edition

The following features depend on Ultimate-only plugins and are gracefully degraded
(class-presence guards prevent crashes, but the functionality is absent):

| Feature | Ultimate dependency | Status |
|---------|---------------------|--------|
| JavaScript completion in `<g:javascript>`/`<r:script>` tags | `JavaScript` plugin | Excluded (no JS completion/resolution) |
| JavaScript formatting in GSP | `JavaScript` plugin | Excluded (JS blocks formatted as raw text) |
| CSS completion in `<style>`/`style="..."` attributes | `com.intellij.css` plugin | Excluded (CSS not recognized in GSP) |
| JSP tag validation (`fmt:formatNumber` etc.) | JSP plugin (Ultimate) | Excluded |
| HTML event attribute completion (`onclick`, `ondblclick`, etc.) | Web/JSP descriptor (Ultimate) | Excluded — CE gets base attributes only |
| HQL injection in GORM methods (`findAll`, `executeQuery`) | Hibernate plugin (Ultimate) | Excluded (HQL not injected) |
| Spring Support plugin integration (bean-name references in GSP, injected-bean typing/completion, Spring facet, model discoverer, `@ContextConfiguration` annotation resolution) | Spring plugin (Ultimate) | Removed in 262.1.0 on **both** editions — the classes live in Spring's base `intellij.spring` module, which the installed plugin's classloader cannot see (NoClassDefFoundError even on Ultimate). The standalone `beans {}` DSL keeps working (see above). |
| Domain Class scope view (graph visualization) | `com.intellij.openapi.graph` (Ultimate platform) | Guarded — view does not open on CE |
| Gradle project importing (Gradle 4.x/5.x/6.x tests) | Gradle plugin test framework (Ultimate) | Excluded — base class not in plain JUnit4 sandbox |
| Maven project importing | Maven plugin (Ultimate) | Excluded |

## What was fixed during CE work

The following issues were discovered and fixed to make CE support safe:

- **`GspFileType` static initializer** — referenced `com.intellij.ultimate.PluginVerifier`
  without a guard. Fixed with `UltimatePluginGuard.invokeStaticIfAvailable()` (reflective
  no-arg static invoke, any Throwable swallowed).
- **`GspCssInjector`** — referenced `CssFileType.INSTANCE` without a guard. On any GSP file
  with a `style=` attribute in CE, this would throw `NoClassDefFoundError`. Fixed with
  `GrailsIntegrationUtil.isCssSupportEnabled()` early return.
- **`DomainClassesRelationsEditorProvider`** — the Domain Class View editor would crash on
  CE when opened (requires `com.intellij.openapi.graph`). Fixed with
  `GrailsIntegrationUtil.isGraphSupportEnabled()` guard in `accept()`.
- **`UltimatePluginGuard`** — new `invokeStaticIfAvailable(className, methodName)` helper
  added for the `GspFileType` use case (static method calls whose class may be absent).
- **`GrailsIntegrationUtil`** — new `isGraphSupportEnabled()` class-presence check for
  the perspectives/graph guard.
- **Maven/Hibernate module extensions moved into module descriptors** — the 2026.2 module
  classloader split made main-plugin config-files (`grails-maven-integration.xml`,
  `grails-hibernate-integration.xml`) invisible to the module classloaders, so the Grails
  project view never appeared. The Maven (5) and Hibernate (3) extension registrations were
  relocated into their module descriptors (`org.apache.grails.intellij.module.maven.xml`,
  `org.apache.grails.intellij.module.hibernate.xml`).
- **`GrailsApplicationProvider` hardened** — `createGrailsApplication()` now tolerates a
  failing provider (per-provider `try/catch`, logged warning) instead of aborting the whole
  application/node tree, with a regression test
  (`GrailsProjectStructureTest.testCreateGrailsApplicationSurvivesFailingProvider`).
- **Spring plugin integration removed (both editions)** — dropped the optional
  `com.intellij.spring` dependency, the `grails-spring-integration.xml` config file, and the
  12 extension classes. Extends into Spring's base module fail with NoClassDefFoundError on
  2026.2; the two self-contained DSL helpers were kept and re-registered in the main
  `plugin.xml`. See the "What does not work" table for the feature list.
- **IC verifier configuration** — JetBrains removed the standalone IC installer channel for
  2025.3+. Fixed with `useInstaller.set(false)` on IC targets. IC verification failure
  level set to `INVALID_PLUGIN` only (MISSING/COMPAT are expected from absent optional deps).

## Running the CE test suite

```bash
# Full CE test suite (~3.5 min, ~988 tests)
./gradlew :plugin:testIdeCe -PplatformEdition=IC

# Run a CE dev IDE (sandbox)
./gradlew :plugin:runIdeCe
```

## Installing locally for testing

The plugin ZIP is written to `plugin/build/distributions/`:

```bash
./gradlew :plugin:buildPlugin
```

Then install via **Settings → Plugins → Install Plugin from Disk** and select the ZIP.

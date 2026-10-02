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

The plugin ships as a single ZIP that loads on IntelliJ IDEA Ultimate with its full feature
set and on IntelliJ IDEA Community Edition 2026.2+ with the features that do not need an
Ultimate plugin. Since 2025.3 JetBrains publishes one IntelliJ IDEA distribution whose free mode
is the Community feature set, so "Community Edition" below also means that distribution
running without an Ultimate subscription. The automated checks use the `ideaIC` artifact;
a manual smoke test on the unified distribution in free mode is still worth doing before a
release.

## How it works

- `plugin.xml` declares hard dependencies only on what Community Edition ships (Java,
  Groovy, Properties, Gradle and the JSP SPI modules the Java plugin bundles).
- Every Ultimate-only integration is a **content module** under `pluginModules/` with its own
  `<dependencies>`. The platform skips a content module whose dependencies are absent, so
  nothing Ultimate-specific is ever loaded on Community Edition, and the main jar compiles
  against the Community API only (see `plugin/build.gradle`).

  | Module | Requires | Provides |
  |--------|----------|----------|
  | `spring` | `com.intellij.spring`, modules `intellij.spring`, `intellij.spring.core` | Injected-bean typing and completion, gutter navigation, Grails artefacts as Spring beans, Spring facet, bean-name references, rename and usage search |
  | `javaee` | `com.intellij.javaee`, `com.intellij.javaee.web`, module `intellij.javaee.web` | Web facet auto-configuration for Grails modules |
  | `database` | `com.intellij.database`, module `intellij.database` | Data-source detection from `DataSource.groovy` |
  | `hibernate` | `com.intellij.hibernate`, `com.intellij.persistence`, `com.intellij.javaee.jpa` | GORM entities as a persistence unit, HQL injection and resolution |
  | `jsp` | `com.intellij.jsp` (Marketplace) | JSP view file type and page reference search |
  | `maven`, `coverage`, `copyright`, `i18n`, `langInjection` | bundled in both editions | Maven import, coverage, copyright, i18n and language injection |

  A plugin-id dependency exposes only that plugin's *main* module to the class loader. The
  Ultimate plugins keep their classes in content modules (`WebFacet` in `intellij.javaee.web`,
  the Spring API in `intellij.spring`, `DataSourceDetector` in `intellij.database`), so a module
  descriptor must name those modules explicitly. The Ultimate test suite runs on a flat
  classpath and Plugin Verifier follows content-module dependencies, so neither notices a
  missing module dependency; the Community verifier run and `runIde` do.
- JavaScript, CSS and Expression Language support stay in the main jar behind class-presence
  guards (`GrailsIntegrationUtil`) and `<depends optional="true">` entries, because they are
  woven into the GSP formatter, PSI and syntax highlighter. Those guarded references are the
  only ones the Community verifier is told to ignore (`plugin/verifier/community-ignored-problems.txt`).

## What works in Community Edition

- GSP parsing, highlighting, formatting (JavaScript blocks fall back to raw text) and
  structure view
- Tag libraries: resolution, completion, named-argument references, namespace priority,
  attribute completion
- Domain classes and GORM: navigation, dynamic finders, criteria and detached criteria,
  constraints, named queries
- Controllers, services, views and templates: recognition, navigation, `request`/`response`
  members, `render`/`respond` support
- Project structure: Grails project detection, the Grails view pane, artefact wizards
- Run configurations, the Grails console and the Grails Forge project wizard
- The `beans {}` DSL in `resources.groovy` and `doWithSpring` closures
  (`GrailsResourcesGroovyMemberContributor` does not depend on the Spring plugin)
- The Grails 8 compile-time beans DSL (`@GrailsBeans`, and the implicit `beans` property of plugin
  descriptors, the `Application` class and unit tests): declarations, qualifier chains and shared
  `field`/`method` members (`GrailsBeansDslMemberContributor`). Registering the declared beans in the
  Spring model is part of the `spring` module
- Gradle and Maven importing, coverage, copyright, i18n, language injection

## What does not work in Community Edition

| Feature | Needs | Behaviour on Community Edition |
|---------|-------|--------------------------------|
| Spring Support integration (see the `spring` module above) | Spring plugin | Module skipped |
| Web facet auto-configuration | Jakarta EE plugins | Module skipped |
| Data-source detection | Database Tools plugin | Module skipped |
| GORM entities as a persistence unit, HQL injection in `find`/`executeQuery` | Hibernate, persistence and JPA plugins | Module skipped |
| JavaScript completion, resolution and formatting inside GSP | JavaScript plugin | Falls back to plain text |
| CSS completion in `<style>` and `style="…"` | CSS plugin | Not injected |
| `${ }` delimiter colouring from the EL colour scheme | Expression Language plugin | Delimiters unstyled |
| JSP tag validation and TLD taglib completion | JSP plugin (Marketplace) | Module skipped |
| Domain class diagram | Graph API (Ultimate platform) | Editor does not open |
| Legacy Grails 2 `web-app` node icon | Java EE icons | Platform folder icon |

## Verifying a change

```bash
# Ultimate (default): full test suite and Plugin Verifier against the recommended IDE builds
./gradlew test
./gradlew verifyPlugin

# Community Edition: verifier against ideaIC (fails on invalid plugin, on any unresolved class
# not in the guarded list, and on an unresolved required dependency), the Community-safe test
# set on an ideaIC sandbox, and a sandbox IDE for manual checks (downloads ~1GB once)
./gradlew verifyPlugin -PplatformEdition=IC
./gradlew :plugin:testIdeCe -PplatformEdition=IC
./gradlew :plugin:runIdeCe -PplatformEdition=IC
```

Tests that need an Ultimate plugin are tagged `@Category(UltimateOnlyTest.class)` (class or
method level) and are excluded from `testIdeCe`. The nightly GitHub Actions job runs the
Community verifier and test set; pull-request builds run the Ultimate suite and verifier.

## Adding a feature that needs an Ultimate plugin

1. Put the code in the matching module under `pluginModules/` (or add a module: copy
   `pluginModules/database`, add it to `settings.gradle` and to the `pluginModule` list and
   `<content>` block of the main plugin).
2. Declare every plugin **and** content module the code uses in the module descriptor's
   `<dependencies>`; find the jar with `unzip -l <IDE>/plugins/<dir>/lib/modules/*.jar`.
3. Register the extensions in the module descriptor, not in `plugin.xml`.
4. Run `./gradlew verifyPlugin -PplatformEdition=IC`; a new unresolved reference there means
   the main jar reaches for an Ultimate class.

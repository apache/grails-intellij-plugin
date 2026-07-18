# Improvement Plan — grails-intellij-plugin

Companion to `MIGRATION-PLAN.md`. That plan makes the repo ASF-compliant; this plan
identifies functional gaps and sequences how to close them. Every phase below was
detailed by examining the **grails-core monorepo** (surveyed 2026-07-07, `main` =
`8.0.0-SNAPSHOT`); concrete class names, coordinates, and DSLs cited come from that
source tree.

**Two-plugin strategy** (per MIGRATION-PLAN amendment 2026-07-07): a *legacy* plugin
(id `org.intellij.grails`, forked at the compliance tag, Grails 2.x–6.x, maintenance
only) and a *new* plugin (fresh Apache id, Grails 7+ only). Unless marked otherwise,
the phases below target the **new** plugin; the legacy plugin receives only Phase 1
(platform health) on a recurring basis.

> Version note: grails-core `main` is 8.0.0-SNAPSHOT (Groovy 5.0.7, Spring Boot 4.1,
> Java 21, Gradle 9.6, Hibernate 7 default). Grails 7.x is the current release line
> and the first under `org.apache.grails` coordinates. Where 7.x and 8.x differ
> (artifact renames, gradle plugin ids), Phase 0 must verify against a real 7.0.x app
> — the facts below are authoritative for 8.x and directionally right for 7.x.

## What we have today (surveyed 2026-07-07)

- ~635 main sources (525 Java, 110 Kotlin) + 11 submodules; 184 test classes.
- Strong core: full GSP language (lexer/parser/PSI, formatting, folding, completion,
  HTML/Groovy/CSS/JS injection), 20+ Groovy member contributors (GORM criteria, named
  queries, constraints), artefact handlers, URL-mapping references, Spring bean
  discovery, run configurations, project view pane, GSON views support, templates.
- Dual-era structure model: `Grails2Application`/`OldGrails*` (BuildConfig.groovy,
  Gant) alongside `Grails3*` (Gradle-based).
- Era artifacts: `pluginSupport/` for Searchable, Shiro, WebFlow, Resources (dead);
  `testdata/` mock Grails **1.x** JARs.
- Ultimate-only (`com.intellij.modules.ultimate` + javaee/persistence/jsp/spring/
  database deps). Targets platform 2025.3, IntelliJ Platform Gradle Plugin 2.11.0.

---

## Phase 0 — Baseline audit (applies to the shared codebase, pre-fork)

Produce `GAPS.md` (or one GitHub issue per finding, labeled `audit`) before changing
anything. Concrete checklist:

**0.1 Build & platform debt**
- [ ] `./gradlew check` — record failing/skipped tests per module.
- [ ] `./gradlew verifyPlugin` against 2025.3 + latest EAP — inventory every
      deprecated/internal/removed API usage (this is the legacy plugin's recurring
      maintenance surface, so severity-rank it).
- [ ] Confirm the `gen/` GSP lexer sources regenerate from their JFlex inputs; document
      the regeneration command in `DEVELOPMENT.md`.

**0.2 Real-app walkthrough** — generate three apps and open each with the plugin:
1. Grails **7.0.x** release (via `https://start.grails.org` — forge production
   endpoint) with the `web` application type,
2. Grails **8.0.0-SNAPSHOT** (forge snapshot endpoint
   `https://grailsforge-snapshot-cjmq3uyfcq-uc.a.run.app/`),
3. Grails **6.x** (legacy baseline, for the legacy plugin's regression record).

Walk this checklist per app and record works / broken / missing:
- [ ] Project recognized as Grails; `grails-app/*` project view pane renders
      controllers/domain/services/taglib/views/conf/i18n/**init**/**utils**/assets
      groups (init/ and utils/ are conventions the plugin may predate).
- [ ] `Application.groovy` in `grails-app/init/` (extends
      `grails.boot.config.GrailsAutoConfiguration`, runs via `GrailsApp.run`) is
      recognized as the run entry point.
- [ ] Domain class in `grails-app/domain/`: GORM static/instance member completion
      (see Phase 2.3 list), constraints block completion, mapping block completion,
      dynamic finder resolution, `where {}` closure typing.
- [ ] Data Service interface with `@grails.gorm.services.Service(Book)`: implemented-
      method recognition (no "cannot instantiate interface" errors, method-convention
      completion).
- [ ] Controller: action navigation, `respond`/`render`, interceptor `match()`/
      `matchAll()` DSL, `UrlMappings.mappings` closure completion.
- [ ] GSP: `g:` tag completion + navigation to taglib closures; `f:` namespace from
      grails-fields; layout tags; `asset:` taglib (asset-pipeline).
- [ ] `.gson` views: `json {}` / `model {}` DSL, `g`/`tmpl`/`hal`/`jsonapi` helpers;
      `.gml` markup views (expected: **no support at all** — confirm).
- [ ] `application.yml`: completion for `grails.*` keys; `runtime.groovy`/
      `application.groovy` recognized; `spring/resources.groovy` bean DSL.
- [ ] Run/debug: `bootRun` launch + debug attach, unit test run (Spock 2.4 traits),
      `integrationTest` task (from `TestPhasesGradlePlugin` source set
      `src/integration-test/groovy`), `@Integration` test debug.
- [ ] New-artefact actions produce Grails-7-correct file stubs (compare against
      `grails-profiles/web/templates/artifacts/` in grails-core).
- [ ] Version/framework detection: does the plugin light up at all when only
      `org.apache.grails` coordinates are present? (Expected failure: detection almost
      certainly keys on `org.grails` group ids — confirm exact detection code paths in
      `Grails3ApplicationProvider`, `GrailsUtils`, SDK/library detection.)

**0.3 Scoping counts (feed Phases 3–4)**
- [ ] Count references from core packages into Ultimate-only APIs (spring, javaee,
      persistence, database plugins) — data for the Community-edition split.
- [ ] Map which classes are reachable only from `OldGrails*`/Gant/BuildConfig paths —
      the safe-deletion list for the new plugin's pruning step.
- [ ] List every stub index / file-based index and its version key (needed for the
      package-rename impact assessment in the new plugin).

---

## Phase 1 — Platform health (both plugins; recurring for legacy)

- [ ] Fix all `verifyPlugin` **error**-level findings (removed/internal APIs).
- [ ] Provide new-UI (20×20) icon variants for the gutter/toolwindow/artefact icons;
      audit `GrailsIconProvider` and `GroovyMvcIcons` (generated) coverage.
- [ ] Verify dumb-mode safety of the heavier contributors (member contributors and
      reference providers should be index-tolerant).
- [ ] Encode the supported-platform window in `pluginVerification.ides` (proposal:
      latest two IDE majors for the new plugin; "current major only, best effort" for
      legacy).
- [ ] Legacy-specific: define the recurring platform-bump procedure (bump
      `sinceBuild`, run verifier, fix errors, release `<branch>.x.y`) and put it in
      the legacy repo's `DEVELOPMENT.md`.

---

## Phase 2 — Grails 7+/8 support (new plugin; the headline work)

### 2.1 Framework detection & versioning

Detection must work from Gradle dependency data (the plugin already has a
`GrailsProjectResolverExtension`); the authoritative signals from grails-core:

- [ ] **Gradle plugin ids**: `org.apache.grails.gradle.grails-app`, `-web`,
      `-plugin`, `-gsp`, `-gson`, `-markup`, `-profile` (Apache era). Legacy signal
      for ≤6: `org.grails.grails-*` ids.
- [ ] **BOM**: `platform("org.apache.grails:grails-bom:<v>")` (variants:
      `grails-bom-hibernate5/-hibernate7/-micronaut`). BOM version = Grails version.
- [ ] **Coordinates**: any `org.apache.grails[.*]:grails-*` dependency ⇒ Apache era.
      Sub-groups to recognize: `org.apache.grails.data` (GORM), `.views`, `.web`,
      `.testing`, `.i18n`, `.gradle`, `.profiles`. `RENAME.md` in grails-core is the
      full old→new artifact map — encode it as a table in one detection class
      (e.g. `grails-plugin-controllers` → `grails-controllers`,
      `grails-datastore-gorm` → `grails-datamapping-core`).
- [ ] **Corroborating signals** (don't hard-require): Groovy group
      `org.apache.groovy` (4/5) vs `org.codehaus.groovy` (≤3); Spring Boot 3 (Grails
      6/7) vs 4.1+ (Grails 8); Java baseline 21 (8.x).
- [ ] Replace/extend `org.jetbrains.plugins.grails.util.Version` handling so 7.x/8.x
      version strings parse and feature-gate correctly (e.g. `.gml` support only when
      `grails-views-markup` present).

### 2.2 Project structure model

- [ ] Single `GrailsApplication` implementation family for Gradle-era apps (7+);
      recognize `grails-app/{controllers,domain,services,taglib,views,conf,i18n,
      init,utils,assets}` plus `src/main/groovy`, `src/test/groovy`,
      `src/integration-test/groovy` (registered by `TestPhasesGradlePlugin`) and
      `src/functional-test/groovy` (functional/Geb phase) source sets.
- [ ] `grails-app/conf`: `application.yml` (primary), `application.groovy`,
      `runtime.groovy`, `logback-spring.xml`, `spring/resources.groovy` (bean DSL —
      keep existing support, verify against Boot 3/4 world).
- [ ] Plugin projects: `*GrailsPlugin` class in `grails-app/init` (or `src/main/
      groovy`), lifecycle hooks `doWithSpring`/`doWithDynamicMethods`/
      `doWithApplicationContext`/`onChange`/`onConfigChange`/`onShutdown` — verify
      completion inside these closures.

### 2.3 Artefact handlers & GORM code insight

Artefact type list to support (from grails-core artefact handlers):
Controller, Domain, Service, TagLib, UrlMappings (fixed name, lives in
`controllers/`), BootStrap (fixed name, `init/`), Codec, Interceptor, Application
(`init/`), plus **Data Service** (new):

- [ ] **Data Services**: `@grails.gorm.services.Service(Domain)` on interfaces /
      abstract classes in `grails-app/services/`. IDE must: suppress
      "unimplemented method" errors (AST-generated impl), complete/resolve
      convention methods (`get`, `list`, `count`, `save`, `delete`, `find*By*`),
      and gutter-mark the generated implementation.
- [ ] **GormEntity member injection** — align the existing NonCodeMemberProcessor
      lists with GORM 9's `org.grails.datastore.gorm.GormEntity` trait. Instance:
      `save/insert/merge/delete/refresh/attach/discard/lock/ident/isDirty/
      getDirtyPropertyNames/addTo*/removeFrom*/instanceOf`. Static: `get/read/load/
      proxy/getAll/list/first/last/count/exists/saveAll/deleteAll/where/whereLazy/
      whereAny/createCriteria/withCriteria/findWhere/findAllWhere/findOrCreateWhere/
      findOrSaveWhere/executeQuery/executeUpdate/withTransaction/withNewTransaction/
      withSession/withNewSession/withStatelessSession` + dynamic finders.
- [ ] **Dynamic finder grammar** (`org.grails.datastore.gorm.finders.*`): prefixes
      `findBy/findAllBy/countBy/listOrderBy/findOrCreateBy/findOrSaveBy`; comparators
      `Equal/NotEqual/InList/NotInList/Between/InRange/Like/Ilike/Rlike/GreaterThan
      [Equals]/LessThan[Equals]/IsNull/IsNotNull/IsEmpty/IsNotEmpty`; `And`/`Or`
      combinators; trailing Map args (`max/offset/sort/order/ignoreCase/fetch/cache/
      lock`).
- [ ] **where/DetachedCriteria**: type the `where {}` closure delegate as
      `grails.gorm.DetachedCriteria<T>` with property-operator DSL (`==`, `!=`, `<`,
      `in`, `like`, …) and `and/or/not/order/join/projections` blocks.
- [ ] **Constraints block**: built-in list from
      `org.grails.datastore.gorm.validation.constraints.*` — nullable, blank, size,
      minSize, maxSize, min, max, range, inList, matches, email, url, creditCard,
      validator, unique, notEqual, scale. (Diff against the plugin's current
      hardcoded list — it dates from Grails 2.)
- [ ] **mapping block**: Hibernate mapping DSL from
      `org.grails.orm.hibernate.cfg.Mapping` (table, id/generator, version, cache,
      sort, batchSize, dynamicInsert/Update, per-property column/sqlType/lazy/
      formula/index).
- [ ] **jakarta, not javax**: domain/JPA integration (the `hibernate/` submodule and
      persistence facet code) must use `jakarta.persistence.*`.

### 2.4 Web layer: GSP, taglibs, views, URL mappings, interceptors

- [ ] **Taglib registry refresh** — the `g:` namespace tag set should be sourced from
      the app classpath (taglib classes with `@TagLib`/namespace field) rather than a
      static list; current core taglibs in grails-core:
      `ApplicationTagLib` (link, createLink, resource, img, set, cookie, header,
      external, join, meta, applyCodec, flashMessages), `FormTagLib` (form,
      textField, passwordField, hiddenField, checkBox, textArea, datePicker,
      submitButton, uploadForm, …), `ValidationTagLib` (message, hasErrors,
      eachError, fieldError, fieldValue), `FormatTagLib` (formatDate, formatNumber,
      formatBoolean, encodeAs), `UrlMappingTagLib` (include, paginate),
      `JavascriptTagLib`, `RenderTagLib` (render, renderException), layout tags
      (layoutBody/layoutHead/layoutTitle/pageProperty/ifPageProperty/applyLayout —
      Sitemesh2 still, packaged as `org.apache.grails:grails-layout`).
- [ ] **`f:` namespace** (grails-fields `grails.plugin.formfields.FormFieldsTagLib`):
      `with`, `all`, `field`, `table`, `display` — grails-fields is now in the
      monorepo, so treat it as first-party.
- [ ] **`.gson` JSON views**: verify against current `grails-views-gson` —
      script base `grails.plugin.json.view.JsonViewWritableScript`, `json {}` /
      `json(list)` entry points, implicit `model {}`, helpers `g`
      (`GrailsJsonViewHelper`), `hal`, `jsonapi`, `tmpl` (`TemplateRenderer`);
      `_partial` template resolution and `/object/_object.gson` convention.
- [ ] **`.gml` markup views — NEW file type**: register extension, inject Groovy
      `MarkupTemplateEngine` DSL, `g` helper (`GrailsViewHelper`). Gate on
      `grails-views-markup` on classpath.
- [ ] **UrlMappings DSL**: completion inside `static mappings = {}` — pattern-string
      entries, `constraints {}`, `group`, `resources`/nested resources, response-code
      mappings (`"404"(...)`) , attrs `controller/action/view/method/namespace`.
      Cross-reference: mapping ↔ controller action ↔ view.
- [ ] **Interceptors**: `grails.artefact.Interceptor` trait members (`match(Map)`,
      `matchAll()`, matcher chain `.excludes/.except`, lifecycle
      `before/after/afterView`) — completion + "matched controllers" navigation.
- [ ] **Codecs**: keep `encodeAs*` dynamic-method support; built-ins HTML, HTML4,
      JavaScript, HTMLJS, URL, Raw (`org.grails.plugins.codecs`).

### 2.5 Config files

- [ ] `application.yml`/`application.groovy` key completion sourced from
      `spring-configuration-metadata.json` on the app classpath (Boot metadata) plus
      the Grails namespace defaults seen in the profile skeletons
      (`grails.codegen.defaultPackage`, `grails.profile`, `grails.views.gsp.*`,
      `info.app.*` with `@…@` Gradle interpolation).
- [ ] Retire `BuildConfig.groovy`/`Config.groovy` analysis from the new plugin
      (moves to legacy only).

### 2.6 Run, test, and command execution

- [ ] Run config delegates to Gradle `bootRun` (Boot 3/4 `BootRun` task; dev-mode PID
      file `run-app.pid`); debug via standard Gradle/JVM attach.
- [ ] Test integration: unit-test traits from `grails-testing-support-*`
      (`grails.testing.services.ServiceUnitTest`, `grails.testing.web.controllers.
      ControllerUnitTest`, `grails.testing.gorm.DomainUnitTest`, `grails.testing.web.
      taglib.TagLibUnitTest`, `grails.testing.web.interceptor.InterceptorUnitTest`,
      `grails.testing.web.UrlMappingsUnitTest`), `@grails.testing.mixin.integration.
      Integration` for integration tests, Geb (`org.apache.groovy.geb:geb-spock`)
      for functional. Spock 2.4. Run line markers on all of these; route
      `integrationTest` through the Gradle task, not a JUnit config.
- [ ] Command layer: replace `Grails3CommandProvider` sources with (a) profile-yml
      commands (`create-controller`, `create-service`, `create-taglib`,
      `create-interceptor`, `create-domain-class`, `create-integration-test` from
      `grails-profiles/*/commands/`) and (b) forge-cli commands
      (`org.grails.forge.cli.command.Create*Command`, incl. `create-job`,
      `create-scaffold-controller` era equivalents). Scaffolding: `@grails.plugin.
      scaffolding.annotation.Scaffold` recognition + `generate-*` commands.
- [ ] New-project wizard: call the forge HTTP API (`https://start.grails.org`; API
      `/application-types` for types/features/gorm-impls/JDKs, POST `/create` for
      generation) with an offline fallback via `org.grails.forge:grails-forge-cli`.
      This replaces the existing JetBrains-era forge module builder endpoint.

### 2.7 Test fixtures

- [ ] Replace mock Grails 1.x JARs in `testdata/` with fixtures from a pinned
      Grails 7/8 release (align with MIGRATION-PLAN Phase 3's source-release
      decision; prefer test-time fetch of real `org.apache.grails` artifacts).
- [ ] Add whole-project test fixtures for the 7.x layout (web app + plugin project +
      data-service usage) so modern support has regression coverage — today's suite
      guards the legacy era.

---

## Phase 3 — Creating the new plugin (fork + prune + rename)

Per the two-plugin strategy, this is no longer a "should we remove?" debate — it's
the construction step of the new plugin, immediately after the compliance fork:

- [ ] New plugin id (proposal: `org.apache.grails.intellij`) + fresh Marketplace
      listing under the Apache vendor; declare mutual exclusivity with
      `org.intellij.grails` (both directions).
- [ ] Package rename `org.jetbrains.plugins.grails.*` / `org.jetbrains.plugins.
      groovy.mvc.*` → `org.apache.grails.intellij.*`: mechanical, but must update
      every FQN in `plugin.xml`/module XMLs, bump all stub-index version keys
      (inventory from Phase 0.3), and accept fresh persisted settings (new plugin =
      no migration burden).
- [ ] Delete, using the Phase 0.3 reachability map:
      - `Grails2Application`, `OldGrails*` (application, command provider, node
        provider), module-based structure sync paths in
        `MvcModuleStructureSynchronizer` (keep what Gradle-era import still uses),
      - BuildConfig.groovy/Config.groovy analysis, Gant script support,
        `grails-compiler-patch` + `jps-plugin` if they only serve the pre-Gradle
        compile pipeline (verify in Phase 0),
      - `pluginSupport/`: searchable, shiro, webflow, resources (keep assetPipeline,
        spock, buildTestData only if still ecosystem-relevant — asset-pipeline yes,
        spock yes),
      - Grails 1.x/2.x testdata and the tests that exercise deleted paths.
- [ ] Re-run the full test suite + `verifyPlugin`; the survivor test set defines the
      new plugin's guarded behavior.
- [ ] Legacy repo: no code changes beyond the Phase 1 maintenance policy; add a
      README banner and Marketplace description pointing Grails 7+ users to the new
      plugin.

---

## Phase 4 — Audience: Community-edition support (new plugin)

`com.intellij.modules.ultimate` + javaee/jsp/spring/database deps make the plugin
invisible to IntelliJ Community users. Using the Phase 0.3 reference counts:

- [ ] Restructure `plugin.xml` so the core loads on Community: GSP language + editing,
      artefact recognition/navigation, GORM completion, run configs, project view,
      forge wizard.
- [ ] Move to `<depends optional="true" config-file=…>` modules: Spring bean
      integration (`com.intellij.spring`), JPA/persistence + the `hibernate/`
      submodule (`com.intellij.persistence`, `com.intellij.javaee.jpa`), database
      integration (`com.intellij.database`), JSP-adjacent GSP features
      (`com.intellij.jsp`), JS/CSS injection (already optional).
- [ ] The existing submodule layout (`hibernate/`, `langInjection/`, `i18n/`, …) is
      most of the needed seam — the work is breaking compile-time references from
      core packages into Ultimate-only APIs (count known from Phase 0.3).
- [ ] Decide the Marketplace story: one artifact with optional deps (recommended)
      vs. core+addon pair. Default to one artifact unless the audit shows an
      irreducible Ultimate core.

---

## Phase 5 — Feature roadmap (new plugin, post-stabilization)

File as issues, prioritize by community feedback. Grounded candidates:

- [ ] Controller action ↔ GSP view ↔ URL-mapping line markers (the mapping DSL data
      from 2.4 makes the triad resolvable).
- [ ] Data Service gutter: `@Service(Book)` interface method → generated implementer
      semantics (`org.grails.datastore.gorm.services.ServiceImplementer` variants).
- [ ] Inspections: unknown `application.yml` key (vs. Boot metadata), constraint on
      nonexistent property, dynamic finder referencing nonexistent property, taglib
      call with unknown attribute, `@Integration` test in unit source set.
- [ ] Quick-doc for GORM injected members and `g:`/`f:` tags (tag descriptions can be
      generated from the taglib source Javadoc in grails-core).
- [ ] Live/file template refresh from `grails-profiles/*/templates/artifacts/`
      (authoritative artifact stubs), including Data Service and Interceptor
      templates; `.gml` markup-view support (2.4).
- [ ] Refactoring: artefact rename cascades (controller rename → views directory +
      URL mappings + tests).
- [ ] Micronaut-BOM awareness (`grails-bom-micronaut`, `micronaut-context` config
      keys) — low priority until user demand shows.

---

## Phase 6 — Community & sustainability (both repos)

- [ ] Issue templates/labels mirroring grails-core `.github` conventions; triage the
      inherited Marketplace/YouTrack feedback into GitHub issues split across the two
      repos (legacy bugs vs. new-plugin features).
- [ ] `DEVELOPMENT.md` per repo: `runIde` sandbox, test layout, GSP lexer
      regeneration (`gen/` provenance), platform-bump procedure (legacy), release
      procedure pointer to MIGRATION-PLAN Phase 6.
- [ ] Marketplace listings: legacy = renamed "Grails (legacy, Grails 2–6)" with
      pointer; new = Apache branding, fresh screenshots against a Grails 7 app.
- [ ] ≥2 committers named as maintainers per plugin line; legacy sunset criteria
      reviewed yearly (cap `untilBuild` + archive when Grails 6 usage justifies).

---

## Sequencing & dependencies

```
MIGRATION-PLAN Phases 1–4 (compliance, this repo)
        │
        ├── Phase 0 audit (read-only, runs in parallel) ──► GAPS.md
        │
first compliant release (feature-frozen re-release of the JetBrains plugin)
        │
        ├── fork ──► legacy repo (keeps org.intellij.grails) ──► Phase 1 recurring
        │
        └── this repo = new plugin
                Phase 3 (prune + rename + new id)
                Phase 1 (platform health)
                Phase 2 (Grails 7+/8 support)   ◄── grails-core facts above
                Phase 4 (Community edition)
                Phase 5 (features) / Phase 6 (community)
```

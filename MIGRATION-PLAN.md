# ASF Migration Plan — grails-intellij-plugin

Bring `apache/grails-intellij-plugin` (imported from `JetBrains/intellij-obsolete-plugins`)
into compliance with ASF policy and the grails-core conventions, and stand up GitHub
Actions for building and publishing.

Unlike prior plugin migrations, this repo **stays standalone** — it cannot merge into the
grails-core monorepo because it builds against the IntelliJ Platform Gradle Plugin
(`org.jetbrains.intellij.platform` 2.11.0), not the Grails BOM/shared-gradle world. We
therefore *port* the grails-core compliance/CI conventions here rather than rewiring onto
shared config. The `mono-repo-integration` skill's principles still apply where relevant
(license headers, RAT excludes for templates/test data, CI gating publish on tests, no
blind deletion of imported infra).

## Current state (surveyed 2026-07-06)

- **Build**: Gradle Kotlin DSL, root plugin + 11 subprojects (`copyright`, `coverage`,
  `gradle-tooling`, `grails-compiler-patch`, `grails-rt`, `hibernate`, `i18n`,
  `jps-plugin`, `langInjection`, `maven`, `testFramework`). IntelliJ Platform Gradle
  Plugin 2.11.0, Kotlin 2.3.0, platform 2025.3.1 (`sinceBuild=253`), Java 21 (8/11 for the
  runtime/JPS legacy modules). `pluginVersion=253.0.0`.
- **Identity**: plugin id `org.intellij.grails`, vendor **JetBrains**, packages
  `org.jetbrains.plugins.grails`.
- **Licensing**: `LICENSE.txt` = Apache-2.0 full text. **No NOTICE.** UPDATED
  (2026-07-18): the ~909 source files that carried the abbreviated JetBrains one-line
  header (`Copyright 2000-20xx JetBrains s.r.o. and contributors. Use of this source
  code is governed by the Apache 2.0 license.`) now carry the **full standard Apache 2.0
  license header** (the license Appendix boilerplate) with the JetBrains copyright line
  preserved — *not* the ASF "Licensed to the Apache Software Foundation" header. ~44
  files still have no header (testdata fixtures, `.ft`/`.gsp` templates, properties,
  XML). `gen/` is generated code; `testdata/` contains mock Grails 1.x JARs (test
  fixtures, not shipped).
- **CI/publishing**: none. No `.github/`, no `.asf.yaml`, no publish or signing config.

## Open decisions (resolve before/while executing)

1. **Header strategy for JetBrains-copyright files.** RESOLVED (2026-07-18): **no
   software grant / IP clearance is recorded yet**, so the licensing terms in the
   per-file headers must not be changed — the files stay attributed to JetBrains and
   must NOT receive the ASF "Licensed to the Apache Software Foundation" header until a
   grant is provided. What we did instead: expanded every abbreviated JetBrains header
   to the full standard Apache 2.0 license header (Appendix boilerplate) while keeping
   the `Copyright 2000-20xx JetBrains s.r.o. and contributors.` line. This satisfies
   RAT's stock Apache-2.0 detection with no custom matcher. The wholesale ASF-header
   replacement (former "option A") moves to Phase 9 (post-grant follow-up) and is
   blocked on the software grant being recorded with the ASF Secretary.
2. **Plugin id & Marketplace listing.** RESOLVED (2026-07-06): JetBrains has already
   handed the Marketplace listing over. The plugin id `org.intellij.grails` is permanent —
   Marketplace ids cannot be changed on an existing listing, and the IDE matches updates
   by id, so a new id would orphan the entire install base and break `<depends>` in
   third-party plugins. Keep the id forever; update the listing's name/vendor to Apache.
   AMENDED (2026-07-07): under the two-plugin strategy below, the id and existing listing
   stay with the **legacy** plugin line; the new Grails 7+ plugin gets a fresh Apache id.
   The Java packages (`org.jetbrains.plugins.grails.*`) CAN be renamed (nothing external
   keys on them), but it means renaming every class reference in plugin.xml/module XMLs,
   resets FQN-keyed persisted state for users, and forces stub-index version bumps — and
   ASF policy does not require it (cf. Apache Groovy keeping `org.codehaus.groovy`). If
   wanted, do it as a dedicated follow-up effort after this migration, never mixed into
   the compliance commits.
3. **Develocity / TestLens**: grails-core CI wires `DEVELOCITY_ACCESS_KEY` and TestLens.
   Decide whether this repo joins the same Develocity instance; if not, drop those steps.
4. **Snapshot distribution**: attach the plugin ZIP as a workflow artifact on every main
   build (simplest, recommended), and/or publish to a Marketplace `eap`/`snapshot`
   channel.
5. **Versioning**: keep the `<platform-branch>.x.y` scheme (`253.0.0`) or align with a
   Grails-style scheme. Marketplace convention favors the platform-branch scheme — keep it.
6. **Maven publishing** RESOLVED (2026-07-18): **none**. The plugin ZIP is never
   consumed from Maven Central/ASF Nexus, so there is no Maven/Nexus publishing at all.
   Distribution channels are: the GitHub release assets, `dist.apache.org` dev → release
   via svn as part of the PMC vote, and the JetBrains Marketplace convenience binary.
   A trial adoption of the Grails Publish plugin was reverted as unnecessary; two rough
   edges found during that trial are candidate fixes on the grails-gradle-publish **1.x
   branch** if it is ever adopted here: (a) it requires `project.version` at plugin apply
   time, and (b) its pom generation is not configuration-cache compatible. Kept from the
   trial: the project version now uses the standard `version` property in
   gradle.properties (was `pluginVersion`); workflows pass `-Pversion=`.
7. **Signing keys** (added 2026-07-18): the existing Grails release GPG key (from the
   project `KEYS` file) covers all ASF signing — source zip, convenience binary, Maven
   `.asc` files. JetBrains Marketplace plugin signing is a separate mechanism: it
   requires an X.509 certificate chain + RSA private key (PEM), not PGP, so a dedicated
   key/cert pair must be generated (per JetBrains docs) and stored as the
   `CERTIFICATE_CHAIN`/`PRIVATE_KEY`/`PRIVATE_KEY_PASSWORD` secrets.

---

## Two-plugin strategy (added 2026-07-07)

Decision: maintain **two plugins** — a *legacy* plugin forked from this codebase that
keeps JetBrains-platform compatibility for older Grails versions (2.x–6.x), and a *new*
plugin that supports only Grails 7+ (Apache-era coordinates, pruned codebase). This is
feasible; the constraints and the plan:

- **Fork point**: fork *after* Phases 1–4 land in this repo (metadata, headers, RAT,
  identity), so both repos inherit the compliance work and neither redoes header
  expansion. Note: the post-grant ASF header replacement (Phase 9) will have to run in
  **both** repos if the fork happens before the grant lands — keep the replacement
  script shared. Feature divergence starts only after the first compliant release.
- **Repos**: this repo (`apache/grails-intellij-plugin`) becomes the home of the **new**
  plugin; request a second repo from INFRA (suggested: `grails-intellij-plugin-legacy`)
  created as a fork at the compliance tag. Both carry the same `.asf.yaml`/CI shape;
  the legacy repo's workflows are identical minus the feature-oriented jobs.
- **Marketplace identity**:
  - *Legacy* keeps plugin id `org.intellij.grails` and the transferred listing — the
    existing install base auto-updates into it and nobody's Grails 2–6 support silently
    disappears. Listing description gains a prominent pointer to the new plugin.
  - *New* plugin registers a fresh id (suggested: `org.apache.grails.intellij`) and a
    new listing under the Apache vendor. Because it has no persisted-state or listing
    history, it is free to rename packages `org.jetbrains.plugins.grails` →
    `org.apache.grails.intellij.*` from day one — this retires the package-rename
    question deferred in decision 2 (it applies to the new plugin only; legacy never
    renames).
- **Mutual exclusivity**: both plugins register the GSP language, file type, run
  configuration type, and stub indices — the platform cannot load both. Declare the
  conflict (plugin.xml `<incompatible-with>` on each other's id; verify the mechanism
  during implementation — if id-level incompatibility isn't honored on the target
  platform, add a startup check that notifies and disables) and document "install one,
  not both" in both listings. If dual-install demand materializes, the escape hatch is
  extracting a shared GSP-core plugin both depend on — out of scope for now.
- **Support policy** (propose to PMC alongside the fork):
  - Legacy: **maintenance-only**. Platform-compat fixes (`sinceBuild` bumps, Plugin
    Verifier errors), critical bugs. No new features. `verifyPlugin` CI against each new
    IDE major is the tripwire. Versioning continues the `<platform-branch>.x.y` scheme.
    Sunset criteria (e.g. when Grails 6 usage drops, cap `untilBuild` and archive) to be
    revisited yearly.
  - New: all feature work (see `IMPROVEMENT-PLAN.md`; its Phase 3 "legacy pruning"
    becomes the *creation step* of the new plugin — prune Grails 1/2 support, dead
    plugin integrations, and `OldGrails*` structure model immediately after the fork).
- **Cost acknowledged**: double release engineering (two votes, two Marketplace
  publishes) and platform bumps twice a year on the legacy line. Mitigated by the
  maintenance-only policy and shared workflow templates.

## Phase 1 — ASF repo metadata

DONE (2026-07-18): committed as "Add ASF repository metadata" — NOTICE, HEADER,
LICENSE rename, `.asf.yaml` (release environment + branch protection + notifications),
README.md. Also added later: `INSTALL` + `.sdkmanrc` + `gradle-bootstrap/` (source
distro ships without the wrapper jar; `cd gradle-bootstrap && gradle bootstrap`
regenerates it pinned to `.sdkmanrc`, grails-core's pattern).

- **`NOTICE`** (new, required by policy). Model on grails-core's:

  ```
  Apache Grails IntelliJ Plugin
  Copyright 2025-2026 The Apache Software Foundation

  This product includes software developed at
  The Apache Software Foundation (http://www.apache.org/).

  This product includes software originally developed by JetBrains s.r.o.
  and contributors (https://www.jetbrains.com/).
  Copyright 2000-2026 JetBrains s.r.o. and contributors.
  Licensed under the Apache License, Version 2.0.
  ```

  Do **not** say "donated to the ASF" until the software grant is recorded; add that
  wording (confirmed against the grant text) in Phase 9.
- **`HEADER`** (new): copy grails-core's root `HEADER` file verbatim — the canonical ASF
  header text ("Licensed to the Apache Software Foundation (ASF) under one or more
  contributor license agreements…"). Pre-grant it applies to **new files only**
  (decision 1); post-grant it becomes the single source of truth for the wholesale
  replacement in Phase 9.
- **`LICENSE`**: rename `LICENSE.txt` → `LICENSE` (grails-core convention). Create a
  `licenses/` directory only if/when non-Apache third-party assets are identified (none
  found so far — icons are JetBrains-authored and covered by the grant; testdata JARs are
  Apache-2.0 Grails/Spring artifacts and are excluded from the source distro anyway —
  see Phase 3 decision on testdata JARs).
- **`.asf.yaml`**: adapt grails-core's — `description`, `homepage:
  https://grails.apache.org/`, notifications to `commits@grails.apache.org` /
  `notifications@grails.apache.org`, default-branch protection ruleset
  (restrict deletion/force-push), and a `release` deployment environment with the same
  required reviewers (jdaugherty, matrei, jamesfredley, sbglasius) to gate the release
  workflow.
- **`README.md`** (new): what the plugin is, supported IntelliJ versions, build
  instructions, ASF links (no release history section, per grails-core docs convention).
- Optional: `CONTRIBUTING.md`, `CODE_OF_CONDUCT.md` pointers to the main project.

## Phase 2 — License headers (pre-grant form)

One commit: "ASF Compliance - license headers". **Constraint (decision 1): until a
software grant is recorded, headers keep JetBrains attribution and standard Apache 2.0
terms — no ASF licensing language on JetBrains-authored files.**

- DONE (2026-07-18, uncommitted): mechanically expanded every abbreviated JetBrains
  header (the `//`, `/* ... */`, `<!-- -->`, and `#` one-line forms, any year range,
  ~909 files) to the full standard Apache 2.0 header (Appendix boilerplate:
  "Licensed under the Apache License, Version 2.0 …") with the original
  `Copyright 2000-20xx JetBrains s.r.o. and contributors.` line and year range
  preserved. Files that already carried the full old-style (2007/2008) JetBrains Apache
  header were left untouched; a duplicated embedded header in `GspUtil.java` was
  de-duplicated. The expansion script lives in the session scratchpad — commit a copy to
  `etc/bin/expand-jetbrains-headers.py` for auditability.
- DONE (2026-07-18, uncommitted): headered the previously-unheadered JetBrains-authored
  files that RAT flags — grails-rt runtime sources/scripts, `GrailsStandardDSL.gdsl`,
  the non-generated `META-INF/*.xml` descriptors, `liveTemplates/gsp.xml`, `grails.tld`,
  bundle/log4j/gradle `.properties`, and the JPS services file — all with the same
  JetBrains-attributed Apache 2.0 header. Files that are RAT-excluded (testdata
  fixtures, templates, forms, description HTML, `MANIFEST.MF`) were left alone.
  Genuinely new ASF-authored files get the canonical ASF header.
- **New files** authored after the import (by ICLA-covered contributors) use the
  canonical ASF header from the root `HEADER` file — no grant needed for new
  contributions.
- Do **not** header: `gen/**` (generated), `testdata/**` fixtures whose content *is* the
  test input (adding headers can break parser/position-sensitive tests — same reason
  grails-core excludes test resources), file templates under `resources/fileTemplates/**`
  (they materialize inside end-user projects — grails-core's "templates people are
  expected to use" rule), `.form` GUI-builder files (machine-edited XML), inspection/
  intention description HTML (grails-core excludes `**/*.html`).
- Verify with a grep audit: every non-excluded source file contains either the full
  Apache 2.0 header (JetBrains-attributed) or the ASF header (new files).

## Phase 3 — RAT verification in the build

One commit: "ASF Compliance - RAT license audit".

Because Phase 2 expanded the headers to the standard Apache 2.0 boilerplate, RAT's
stock license matcher recognizes them — **no custom JetBrains matcher is needed**.

DONE (2026-07-18, uncommitted): `org.nosphere.apache.rat` 0.8.1 added to the root
`plugins {}` block with a kts `tasks.rat { excludes... }` config (grails-core's shape,
translated; every exclude justified in a comment; `outputs.upToDateWhen { false }`).
`./gradlew rat` passes: 927 files audited, 0 unknown licenses. Notable repo-specific
exclude: `.intellijPlatform/**` (IntelliJ Platform Gradle Plugin's local cache of
generated Ivy descriptors). Remaining details below for reference.

- Add the `org.nosphere.apache.rat` plugin (grails-core pins **0.8.1** via
  `apacheRatVersion` on the buildSrc classpath; this repo has no buildSrc, so declare
  `id("org.nosphere.apache.rat") version "0.8.1"` in the root `plugins {}` block) and port
  grails-core's `gradle/rat-root-config.gradle` shape (this repo is Kotlin DSL; either
  keep a Groovy `gradle/rat-root-config.gradle` applied via `apply(from = ...)` — closest
  to grails-core — or translate to kts).
- Excludes tailored to this repo (mirroring grails-core's categories):
  - generated: `gen/**`
  - test fixtures: `testdata/**` (includes the mock Grails JARs; binary anyway)
  - templates shipped into user projects: `resources/fileTemplates/**`,
    `resources/liveTemplates/**` (verify), `**/*.ft`, template `.gsp`
  - images: `**/*.png`, `**/*.svg`, `**/*.ico`
  - html/json descriptions: `**/*.html`, `**/spring-configuration-metadata.json`-style
    JSON (JSON can't carry headers)
  - infra: `.asf.yaml`, `.github/**`, `**/.gitignore`, `gradlew*`, `**/wrapper/**`,
    `.idea/**`, `**/*.iml`, `out/**`, `build/**` + per-subproject build dirs (use the
    same `rootProject.subprojects.collect{...}` trick), `**/*.form` if not headered
  - `MIGRATION-PLAN.md` (this file) until deleted
- Set `outputs.upToDateWhen { false }` (never cache audits), like grails-core.
- Iterate `./gradlew rat` until clean; every exclude must carry a justification comment
  (grails-core style) — no blanket excludes to make it pass.
- Decision to record: whether the `testdata/**/*.jar` binaries may remain in the
  **source release** at all. ASF source releases must not contain compiled code without
  justification; likely fine as clearly-labeled test fixtures, but flag it for the PMC
  and document in the RAT config comment. Alternative: fetch them at test time.

## Phase 4 — Identity & conventions cleanup

DONE (2026-07-18): committed as "Update plugin vendor and Gradle groups to Apache" —
vendor is Apache Software Foundation (url grails.apache.org), Gradle groups are
`org.apache.grails.intellij*`. Plugin id and extension-point namespaces stay
`org.intellij.grails` per decision 2. Package renames remain out of scope.

- `plugin.xml`: `vendor` → Apache Software Foundation (Grails), vendor URL
  `https://grails.apache.org/`, plugin description/links updated; keep plugin id per
  decision 2.
- Gradle `group` coordinates: `org.intellij.grails` → `org.apache.grails.intellij` (or
  similar) for the module groups — these are internal (nothing publishes to Maven), so
  this is low-risk; record the rename in the repo README. Package renames
  (`org.jetbrains.plugins.grails` → `org.apache...`) are **out of scope** for this
  migration (huge churn, breaks settings/serialized state and plugin.xml extension
  points); revisit later if the PMC wants it.
- Apply the grails-core best-practice conventions that translate: 4-space indent, no
  wildcard imports, Apache header on every **new** file. CodeNarc does not apply (Java/
  Kotlin codebase); consider ktlint/detekt later, not in this migration.

## Phase 5 — GitHub Actions: build & verification CI

DONE (2026-07-18): committed as "Add CI workflows for build, verification, and RAT" —
`gradle.yml` (build+test with plugin ZIP artifact, Plugin Verifier job with
`recommended()` IDEs) and `rat.yml` (grails-core's, adapted to `main`, Develocity/
TestLens dropped per decision 3). All actions pinned to grails-core's SHAs. The
`test.idea.home.path` property is now only applied when the path exists, so `check`
runs on CI. NOT yet validated on a real GitHub runner — first PR will shake it out.

One commit: "ASF Compliance - CI workflows". Follow grails-core's patterns: all actions
**pinned to full commit SHAs**, `concurrency` group per workflow+ref, publish gated on
test jobs via `needs:` **and** `if:` result guards.

- **`.github/workflows/gradle.yml`** — on push/PR to `main` (+ `workflow_dispatch`):
  - `build`: checkout (`actions/checkout` @ pinned v6.0.2), `actions/setup-java`
    (liberica, 21), `gradle/actions/setup-gradle` @ pinned v6.1.0 with
    `cache-provider: basic` (the MIT-licensed provider — grails-core deliberately avoids
    the proprietary default) and Develocity key per decision 3; run
    `./gradlew check buildPlugin`.
  - `verify`: `./gradlew verifyPlugin` (IntelliJ Plugin Verifier against the
    `sinceBuild`/target IDE matrix — configure `pluginVerification.ides` in the build to
    cover 2025.3 and latest EAP; this is the IntelliJ-world analog of grails-core's
    functional matrix and the skill's "reproduce every test axis" rule).
  - `snapshot` (main-branch pushes only): upload the `buildPlugin` ZIP as a workflow
    artifact; optionally `publishPlugin` to a Marketplace snapshot channel (decision 4).
    `needs: [build, verify]` **and** `if:` guards on both results — publishing must be
    blocked when tests fail.
- **`.github/workflows/rat.yml`** — copy grails-core's nearly verbatim (adapt branch
  filter to `main`): run `./gradlew rat`, upload the HTML report artifact, inject the
  report into the job summary.
- Later/optional, mirroring grails-core: `codeql.yml`, dependabot config.

## Phase 6 — GitHub Actions: ASF release & Marketplace publishing

DONE (2026-07-18, initial draft): committed as "Add release workflow and Marketplace
signing config" (Maven/Nexus staging later removed per decision 6). `release.yml` jobs:
`publish` (check/buildPlugin/signPlugin, GPG-sign + sha512, attach ZIP to GitHub
release), `source` (src zip without wrapper/.github, GPG-sign + sha512), `upload` (svn
to dist/dev for the vote), `release` (environment-gated: manual dev->release promotion
via `.github/scripts/releaseDistributions.sh` run locally by the RM, `publishPlugin` to
Marketplace, reporter/email reminders). Secrets needed:
GRAILS_GPG_KEY, GPG_KEY_ID, SVC_DIST_GRAILS_USERNAME/PASSWORD, CERTIFICATE_CHAIN,
PRIVATE_KEY, PRIVATE_KEY_PASSWORD, PUBLISH_TOKEN. Needs a dry-run against a test tag
before first use (Phase 8).

### Secrets chart (all used by `release.yml` only; CI needs none)

| Secret | Used by (job → step) | Value / how to obtain |
|---|---|---|
| `GRAILS_GPG_KEY` | `publish` + `source` → "Set up GPG" | ASCII-armored GPG **private** key of the Grails release key: `gpg --armor --export-secret-keys <keyid>`. Must be passphrase-less (the workflow signs with `--pinentry-mode loopback` and no passphrase). Public key must be in the Grails `KEYS` file on dist.apache.org. Same secret grails-core uses — likely already an org-level secret. |
| `GPG_KEY_ID` | `publish` + `source` → GPG-sign steps | The key id of that same key (`gpg --list-keys --keyid-format long`). Passed as `--default-key`. |
| `SVC_DIST_GRAILS_USERNAME` | `upload` → svn steps | ASF service account username for dist.apache.org svn, provisioned by INFRA for the Grails PMC. Same org secret as grails-core. |
| `SVC_DIST_GRAILS_PASSWORD` | `upload` → svn steps | Password for that service account. (The post-vote dev → release promotion is NOT automated — the release manager runs `.github/scripts/releaseDistributions.sh` locally with their own ASF credentials, per grails-core.) |
| `CERTIFICATE_CHAIN` | `publish` → `signPlugin`; `release` → `publishPlugin` | PEM X.509 certificate chain for JetBrains Marketplace plugin signing. Generate per JetBrains docs (RSA key + cert, self-generated is fine): `openssl req -x509 -newkey rsa:4096 -keyout private.pem -out chain.crt -days 3650`. NOT the GPG key — Marketplace signing is PKI, not PGP. |
| `PRIVATE_KEY` | `publish` → `signPlugin`; `release` → `publishPlugin` | PEM RSA private key matching `CERTIFICATE_CHAIN` (the `private.pem` above, optionally encrypted). |
| `PRIVATE_KEY_PASSWORD` | `publish` → `signPlugin`; `release` → `publishPlugin` | Password for `PRIVATE_KEY` (empty-value secret if the key is unencrypted). |
| `PUBLISH_TOKEN` | `release` → `publishPlugin` | JetBrains Marketplace API token, minted under the ASF-controlled Marketplace vendor account (Profile → My Tokens) that owns the transferred `org.intellij.grails` listing. |

`GITHUB_TOKEN` is provided automatically by Actions (used to upload release assets); no
repository variables (`vars.*`) are required. Secrets for ASF repos are provisioned via
an INFRA Jira ticket (or inherited if already defined at the `apache` org level for
Grails, as grails-core's are).

One commit: "ASF Compliance - release workflow". Modeled on grails-core's `release.yml`
and the `apache/grails-github-actions` composite actions, adapted because the deliverable
is a plugin ZIP, not Maven artifacts (no Nexus staging at all):

ASF policy framing: the **official release is the signed source archive**, voted on by
the PMC and published to `dist.apache.org`; the plugin ZIP on JetBrains Marketplace is a
convenience binary.

- Trigger: GitHub `release` event (tag `vX.Y.Z`), same as grails-core.
- Jobs:
  1. `publish` — build: `apache/grails-github-actions/pre-release@asf` (sets release
     version), `./gradlew buildPlugin`, JetBrains `signPlugin` (Marketplace code-signing
     chain: `CERTIFICATE_CHAIN`, `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD` secrets), attach
     ZIP to the GitHub release (`softprops/action-gh-release` @ pinned SHA).
  2. `source` — produce the source distribution (git-archive based zip excluding
     `.github` etc.), generate `sha512`, GPG-sign (`.asc`) with the release manager's key
     from the Grails `KEYS` file.
  3. `upload` — `needs: [publish, source]`; svn-commit source + convenience artifacts to
     `dist.apache.org/repos/dist/dev/grails/` for the vote.
  4. `release` — `environment: release` (required reviewers from `.asf.yaml`; this is the
     post-vote gate): move dev → `dist/release`, then `./gradlew publishPlugin` to
     JetBrains Marketplace (`PUBLISH_TOKEN` / `ORG_GRADLE_PROJECT_...PublishingToken`
     secret — the listing is already transferred, so this just needs a token minted
     under the ASF-controlled Marketplace account and stored as a repo secret).
  5. `close` — `apache/grails-github-actions/post-release@asf` (bump to next snapshot,
     housekeeping).
- Secrets to provision (INFRA/PMC): GPG signing key material, `GRAILS_GHTOKEN`-style
  token if needed, JetBrains Marketplace token + code-signing certs, Develocity key
  (optional).
- Also add `release-abort`/`release-notes` style helpers later if useful; not required
  for the first release.

## Phase 7 — Artifact compliance

DONE (2026-07-18): LICENSE/NOTICE are packaged into the plugin jar META-INF via
`processResources` (verified in the built ZIP). Stretch goals below remain open.

- Ensure `LICENSE` and `NOTICE` are packaged **inside the plugin ZIP / jar META-INF**
  (grails-core gets this from its shared build-logic plugins; here add a small Gradle
  convention — `processResources`/`buildPlugin` copy spec — in the root build).
- Optional stretch goals, ported from grails-core if the PMC wants parity: CycloneDX
  SBOM (`SbomPlugin.groovy`-style `LICENSE_MAPPING` for transitives), reproducible-build
  config (stable timestamps/ordering in the ZIP), `verify.sh`-style release verification
  scripts. Not blockers for the first compliant release.

## Phase 8 — Verify

- [ ] `./gradlew rat` passes locally and in CI; every exclude justified in a comment.
- [ ] Header audit grep: no non-excluded source file lacks the full Apache 2.0 header
      (JetBrains-attributed on imported files, ASF on new files).
- [ ] No imported file carries ASF licensing language (grep for "Licensed to the Apache
      Software Foundation" must only hit files authored post-import) — guard until the
      software grant is recorded.
- [ ] `NOTICE` + `LICENSE` present at root **and** inside the built plugin ZIP.
- [ ] `.asf.yaml` valid (notifications, branch protection, release environment).
- [ ] `./gradlew check buildPlugin verifyPlugin` green in CI on a PR.
- [ ] Snapshot publish job gated on build+verify via `needs:` and `if:` result guards.
- [ ] All workflow actions pinned to commit SHAs.
- [ ] Dry-run the release workflow to a test tag (skip the `release` environment job).
- [ ] PMC sign-off on: pre-grant header posture (JetBrains-attributed Apache 2.0),
      plugin id/Marketplace transfer, testdata JARs in the source release.

## Phase 9 — Post-grant follow-up (BLOCKED until software grant recorded)

Do **not** start any of this until the JetBrains software grant is recorded with the
ASF Secretary and IP clearance is filed (incubator IP-clearance form, even for a
non-incubating PMC receiving a codebase).

- Wholesale replacement of the JetBrains-attributed Apache 2.0 headers with the
  canonical ASF header from `HEADER` (script in `etc/bin/`, one mechanical commit —
  the former "option A"). Run in the legacy repo too if the fork has happened.
- Move the JetBrains copyright into `NOTICE` and add the "donated to the ASF" wording,
  confirmed against the grant text.
- Update the Phase 8 grep guard: the ASF header becomes required on all non-excluded
  files; the JetBrains-attributed form should no longer appear.
- Revisit RAT config: no changes expected (both header forms are Apache-2.0 to RAT),
  but re-run the audit after the replacement.

## Suggested commit sequence

1. `ASF Compliance - license headers` (expand JetBrains headers to full Apache 2.0 —
   done in working tree + commit the `etc/bin` expansion script)
2. `ASF Compliance - repository metadata` (NOTICE, LICENSE rename, .asf.yaml, README,
   HEADER)
3. `ASF Compliance - RAT license audit` (rat plugin + config, passing)
4. `ASF Compliance - plugin identity` (vendor, groups)
5. `ASF Compliance - CI workflows` (gradle.yml, rat.yml)
6. `ASF Compliance - release workflow` (release.yml, packaging LICENSE/NOTICE)
7. *(post-grant, Phase 9)* `ASF Compliance - ASF license headers` (replacement + NOTICE
   update)

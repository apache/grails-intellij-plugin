# ASF Migration Plan — grails-intellij-plugin

Bring `apache/grails-intellij-plugin` (imported from `JetBrains/intellij-obsolete-plugins`)
into compliance with ASF policy and the grails-core conventions, and stand up GitHub
Actions for building and publishing.

Unlike prior plugin migrations, this repo **stays standalone** — it cannot merge into the
grails-core monorepo because it builds against the IntelliJ Platform Gradle Plugin
(`org.jetbrains.intellij.platform`), not the Grails BOM/shared-gradle world. We therefore
*port* the grails-core compliance/CI conventions here rather than rewiring onto shared
config. The `mono-repo-integration` skill's principles still apply where relevant
(license headers, RAT excludes for templates/test data, CI gating publish on tests, no
blind deletion of imported infra).

**Status: Phases 1–7 are complete** (see "Completed work" below). What remains is Phase 8
(the parts that need a real GitHub runner and a PMC vote) and Phase 9 (blocked on the
software grant). For the current build layout see [AGENTS.md](AGENTS.md#project-structure);
for the release procedure see [RELEASE.md](RELEASE.md).

## State at import (surveyed 2026-07-06)

Kept as the record of what was inherited — this is **not** current state.

- **Build**: Gradle Kotlin DSL, root plugin + 11 flat subprojects. IntelliJ Platform
  Gradle Plugin 2.11.0, Kotlin 2.3.0, platform 2025.3.1 (`sinceBuild=253`), Java 21 (8/11
  for the runtime/JPS legacy modules). `pluginVersion=253.0.0`.
- **Identity**: plugin id `org.intellij.grails`, vendor **JetBrains**, packages
  `org.jetbrains.plugins.grails`.
- **Licensing**: `LICENSE.txt` = Apache-2.0 full text, **no NOTICE**, ~909 sources
  carrying the abbreviated JetBrains one-line header (`Copyright 2000-20xx JetBrains
  s.r.o. and contributors. Use of this source code is governed by the Apache 2.0
  license.`).
- **CI/publishing**: none. No `.github/`, no `.asf.yaml`, no publish or signing config.

## Decisions of record

Numbering is stable — later phases cite these by number.

1. **Header strategy for JetBrains-copyright files.** RESOLVED (2026-07-18): **no
   software grant / IP clearance is recorded yet**, so per-file licensing terms must not
   change. Files stay attributed to JetBrains and must NOT receive the ASF "Licensed to
   the Apache Software Foundation" header until a grant is provided. Instead every
   abbreviated JetBrains header was expanded to the full standard Apache 2.0 header
   (Appendix boilerplate) keeping the `Copyright 2000-20xx JetBrains s.r.o. and
   contributors.` line, which satisfies RAT's stock Apache-2.0 detection with no custom
   matcher. Wholesale ASF-header replacement is Phase 9, blocked on the grant.
2. **Plugin id & Marketplace listing.** RESOLVED (2026-07-06): the listing is already
   transferred. `org.intellij.grails` is permanent — Marketplace ids cannot change on an
   existing listing and the IDE matches updates by id, so a new id would orphan the
   install base and break third-party `<depends>`. AMENDED (2026-07-07): under the
   two-plugin strategy the id and existing listing stay with the **legacy** line; the new
   Grails 7+ plugin gets a fresh Apache id. Java packages CAN be renamed (nothing
   external keys on them) but ASF policy does not require it (cf. Apache Groovy keeping
   `org.codehaus.groovy`); see Phase 3 of `IMPROVEMENT-PLAN.md`.
3. **Develocity / TestLens.** RESOLVED: **not adopted.** grails-core wires
   `DEVELOCITY_ACCESS_KEY` and TestLens; those steps are dropped here, so CI needs no
   Develocity secret.
4. **Snapshot distribution.** RESOLVED: the plugin ZIP is attached as a **workflow
   artifact** on every build (`gradle.yml`). No Marketplace `eap`/`snapshot` channel
   publishing, so CI performs no publishing at all.
5. **Versioning.** RESOLVED: keep the `<platform-branch>.x.y` scheme (currently
   `262.0.0`), which matches Marketplace convention.
6. **Maven publishing.** RESOLVED (2026-07-18): **none.** The plugin ZIP is never
   consumed from Maven Central/ASF Nexus. Distribution is: GitHub release assets,
   `dist.apache.org` dev → release via svn as part of the PMC vote, and the JetBrains
   Marketplace convenience binary. A trial of the Grails Publish plugin was reverted as
   unnecessary; two rough edges found are candidate fixes on the grails-gradle-publish
   **1.x branch** if ever adopted here: (a) it requires `project.version` at plugin apply
   time, (b) its pom generation is not configuration-cache compatible. Kept from the
   trial: the project version uses the standard `version` property in gradle.properties
   (was `pluginVersion`); workflows pass `-Pversion=`.
7. **Signing keys** (added 2026-07-18): the existing Grails release GPG key (from the
   project `KEYS` file) covers all ASF signing. JetBrains Marketplace plugin signing is a
   separate mechanism — X.509 certificate chain + RSA private key (PEM), not PGP — so a
   dedicated key/cert pair must be generated per JetBrains docs and stored as the
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

## Completed work (Phases 1–7)

All seven landed between 2026-07-18 and the restructure. The how-to prose has been removed;
what follows is the record of what exists, with the live detail now kept in the files
themselves.

| Phase | Landed as | What exists now |
|---|---|---|
| 1 — ASF repo metadata | "Add ASF repository metadata" | `NOTICE`, `HEADER`, `LICENSE` (renamed from `LICENSE.txt`), `.asf.yaml` (release environment, branch protection, notifications), `README.md`. Later: `INSTALL`, `.sdkmanrc`, `gradle-bootstrap/` so the source distro can regenerate the wrapper (`cd gradle-bootstrap && gradle bootstrap`). |
| 2 — License headers (pre-grant) | "ASF Compliance - license headers" | ~909 abbreviated JetBrains headers expanded to the full Apache 2.0 boilerplate with the JetBrains copyright preserved, per decision 1. Previously-unheadered JetBrains files headered too. Not headered, by design: `gen/**`, `testdata/**`, `fileTemplates/**`, `.form`, inspection-description HTML. |
| 3 — RAT verification | "ASF Compliance - RAT license audit" | `org.nosphere.apache.rat` 0.8.1, configured in the `rat` convention plugin with every exclude justified inline and audits never cached. `./gradlew rat` clean. |
| 4 — Identity & conventions | "Update plugin vendor and Gradle groups to Apache" | Vendor = Apache Software Foundation (grails.apache.org); Gradle groups `org.apache.grails.intellij*`. Plugin id and EP namespaces stay `org.intellij.grails` per decision 2. |
| 5 — Build & verification CI | "Add CI workflows for build, verification, and RAT" | `gradle.yml` (build + test + plugin-ZIP artifact; Plugin Verifier job) and `rat.yml`. Also since added: `coverage.yml`, `codeql.yml`, `vulnerability-scan.yml`. All actions pinned to full SHAs. |
| 6 — Release & Marketplace publishing | "Add release workflow and Marketplace signing config" | `release.yml` (`publish` → `source` → `upload` → `release`), `release-abort.yml`, checked-in vote/result/announcement templates under `.github/vote_templates/`. Nexus staging removed per decision 6. |
| 7 — Artifact compliance | — | `LICENSE`/`NOTICE` packaged into the plugin jar's `META-INF` (verified in the built ZIP). Reproducible-build pinning and the `etc/bin/verify*.sh` release-verification scripts, listed as Phase 7 stretch goals, are also done — see [RELEASE.md](RELEASE.md). CycloneDX SBOM remains **not** done. |

Still open from Phase 7's stretch list:

- [ ] CycloneDX SBOM with a `LICENSE_MAPPING` for transitives (grails-core parity). Not a
      blocker for a first compliant release.

Still open from Phase 3:

- [ ] PMC decision to record: whether the `testdata/**/*.jar` binaries may remain in the
      **source release**. ASF source releases should not carry compiled code without
      justification; they are clearly-labeled test fixtures, but flag it for the PMC.
      Alternative: fetch them at test time (see `IMPROVEMENT-PLAN.md` 2.7).

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



## Phase 8 — Verify

Verified locally (2026-07-26):

- [x] `./gradlew rat` passes; every exclude carries an inline justification.
- [x] Header audit: every non-excluded source carries the full Apache 2.0 header. The 38
      files without one are all under `plugin/testdata/`, which Phase 2 deliberately does
      not header and RAT excludes.
- [x] No imported file carries ASF licensing language. The 28 hits for "Licensed to the
      Apache Software Foundation" are all post-import files — `build-logic/**`,
      `gradle-bootstrap/`, and seven sources authored in this repo — so the pre-grant
      guard holds.
- [x] `NOTICE` + `LICENSE` present at root and inside the built plugin ZIP (in the
      composed jar's `META-INF`).
- [x] All workflow actions pinned to full commit SHAs.
- [x] `./gradlew check buildPlugin verifyPlugin` green locally.

Cannot be closed without a real runner, a tag, or the PMC:

- [ ] `./gradlew check buildPlugin verifyPlugin` and `rat` green **in CI on a PR** —
      nothing has been pushed yet, so no workflow has ever executed on a GitHub runner.
      This is the single largest untested area of the migration.
- [ ] `.asf.yaml` accepted by INFRA (notifications, branch protection, release
      environment). Structurally complete but never applied.
- [ ] Dry-run the release workflow to a test tag (skip the `release` environment job).
- [ ] PMC sign-off on: pre-grant header posture (JetBrains-attributed Apache 2.0),
      plugin id/Marketplace transfer, testdata JARs in the source release.

Dropped as no longer applicable:

- ~~Snapshot publish job gated on build+verify via `needs:`/`if:`~~ — decision 4 settled on
  a workflow artifact rather than channel publishing, so CI publishes nothing and there is
  no job to gate.

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

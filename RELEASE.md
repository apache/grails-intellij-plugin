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

# Apache Grails IntelliJ Plugin — Release Process

This document describes how a version of the Apache Grails IntelliJ plugin is released.

**ASF framing.** The official release voted on by the PMC is the **signed source
distribution** published to `dist.apache.org`. The plugin ZIP on the JetBrains
Marketplace is a *convenience binary* built from that same tagged source. Both must be
reproducible from the tagged commit — see
[Verifying a Reproducible Build](#verifying-a-reproducible-build) for why that is a
requirement and not just good practice.

The pipeline is encoded in [`.github/workflows/release.yml`](.github/workflows/release.yml)
and is triggered when a **GitHub Release is published** for a tag. It runs in four stages,
described in sections 2–5 below.

## Contents

- [Versioning](#versioning)
- [1. Prerequisites](#1-prerequisites)
- [2. Cut the release](#2-cut-the-release)
- [3. Build & stage the plugin ZIP (`publish` job)](#3-build--stage-the-plugin-zip-publish-job)
- [4. Create the source distribution (`source` job)](#4-create-the-source-distribution-source-job)
- [5. Stage to dist.apache.org (`upload` job)](#5-stage-to-distapacheorg-upload-job)
- [6. Verify the staged artifacts](#6-verify-the-staged-artifacts)
- [7. Vote](#7-vote)
- [8. Publish the release (`release` job)](#8-publish-the-release-release-job)
- [Rollback](#rollback)
- [Verifying a Reproducible Build](#verifying-a-reproducible-build)
- [Appendix: GPG & the KEYS file](#appendix-gpg--the-keys-file)
- [Appendix: Verification from a container](#appendix-verification-from-a-container)
- [Appendix: Release secrets](#appendix-release-secrets)
- [Appendix: Why the build must be reproducible](#appendix-why-the-build-must-be-reproducible)

## Versioning

The plugin does **not** follow the Grails framework version scheme. It follows the
JetBrains Marketplace convention of versioning against the IntelliJ Platform branch it
targets:

```
<platform-branch>.<minor>.<patch>        e.g. 262.0.0
```

- **platform-branch** — the IntelliJ Platform build branch, matching `pluginSinceBuild` in
  [`gradle.properties`](gradle.properties). `262` is the 2026.2 branch.
- **minor** — incremented for feature work within a platform branch.
- **patch** — incremented for bug-fix-only releases within a platform branch.

A new IntelliJ Platform branch means a new `platform-branch` segment, not a major bump:
the 2026.3 line would start at `263.0.0`. Release tags are the version prefixed with `v`
(`v262.0.0`), and the workflow derives the version by stripping that prefix
(`VERSION=${TAG#v}`).

The version lives in `gradle.properties` and is overridden per release with
`-Pversion=${VERSION}`, so the checked-in value does not have to be bumped before tagging.

## 1. Prerequisites

- The release tag points at the exact commit to be released.
- Marketplace signing / publishing secrets are configured: `CERTIFICATE_CHAIN`,
  `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `PUBLISH_TOKEN`.
- ASF signing + dist secrets are configured: `GRAILS_GPG_KEY`, `GPG_KEY_ID`,
  `SVC_DIST_GRAILS_USERNAME`, `SVC_DIST_GRAILS_PASSWORD`.
  See [Appendix: Release secrets](#appendix-release-secrets).
- The `release` environment reviewers are set in [`.asf.yaml`](.asf.yaml). GitHub does not
  allow private groups as approvers, so approvers are listed individually and there is a
  hard limit of 6. Add yourself before starting if you are not already listed.
- The JDK is pinned by [`.sdkmanrc`](.sdkmanrc); CI reads the version from it so local and
  CI builds match. See [INSTALL](INSTALL) for local build setup.
- No planning documents that are excluded from the license audit
  (`MIGRATION-PLAN.md`, `IMPROVEMENT-PLAN.md`) should still be present for a final
  release; they are listed as RAT excludes in [`build.gradle.kts`](build.gradle.kts) and
  are meant to be removed before the first release.

## 2. Cut the release

1. Go to <https://github.com/apache/grails-intellij-plugin/releases> and click
   **Draft a new release**.
2. Create the tag as `v<version>` (for example `v262.0.0`), targeting the branch being
   released from.
3. Leave **Previous tag** on *auto* and click **Generate release notes**, then edit them
   per project agreement.
4. Check **Set as a pre-release** for milestone / release-candidate versions.
5. Click **Publish release**. This triggers the `Release` workflow.

The first three jobs (`publish`, `source`, `upload`) run without approval. The final
`release` job is gated on the `release` GitHub environment and will wait for a reviewer.

## 3. Build & stage the plugin ZIP (`publish` job)

1. Establish `VERSION` from the tag (`${TAG#v}`).
2. `./gradlew check buildPlugin -Pversion=${VERSION}` — full build, tests, and plugin ZIP.
3. `./gradlew signPlugin` — JetBrains Marketplace signature.
4. Rename the signed ZIP to `apache-grails-intellij-plugin-${VERSION}-bin.zip`.
   The build names the ZIP after the IntelliJ plugin name (`rootProject.name`), not the
   ASF distribution name, so the job globs for `*-${VERSION}-signed.zip` and fails if that
   does not match exactly one file.
5. GPG-detached-sign (`.asc`) and checksum (`.sha512`).
6. Upload the ZIP, `.asc`, and `.sha512` to the GitHub Release.

## 4. Create the source distribution (`source` job)

1. Check out the tag, then remove files that must not ship in a source release
   (`.git`, `.github`, `.asf.yaml`, `gradlew`/`gradlew.bat`, `gradle/wrapper`).
2. Zip to `apache-grails-intellij-plugin-${VERSION}-src.zip`.
3. GPG-detached-sign (`.asc`) and checksum (`.sha512`).
4. Upload all three to the GitHub Release.

The Gradle Wrapper jar is removed because ASF source releases may not contain compiled
binaries. [`gradle-bootstrap`](gradle-bootstrap) regenerates it — that is why it ships in
the source distribution, and why `verify-distributions.sh` asserts both that
`gradle-bootstrap` is present and that no `gradle-wrapper.jar` is.

## 5. Stage to dist.apache.org (`upload` job)

1. Ensure `dist/dev/grails/INTELLIJ` exists in the ASF dist SVN.
2. Upload the source distribution under `dist/dev/grails/INTELLIJ/${VERSION}/sources/`
   and the convenience binary under `.../${VERSION}/distribution/`, each with its
   signature and checksum.

At this point the artifacts are staged but nothing is public. **Verify them before
starting the vote.**

## 6. Verify the staged artifacts

Everything in this section can be run at once:

```bash
etc/bin/verify.sh v262.0.0 /tmp/grails-ij-verify
```

That script chains the four steps below and prints the manual checks it cannot perform.
Add `--skip-reproducible` to stop before the slow rebuild step.

Because the rebuild comparison is sensitive to host-OS differences, prefer running it in
the container described in
[Appendix: Verification from a container](#appendix-verification-from-a-container).

### 6.1 Download the staged artifacts

```bash
etc/bin/download-release-artifacts.sh v262.0.0 /tmp/grails-ij-verify
```

Fetches the source and binary distributions (each with `.asc` and `.sha512`) from
`https://dist.apache.org/repos/dist/dev/grails/INTELLIJ/${VERSION}`, plus the Grails
`KEYS` file.

### 6.2 Verify checksums, signatures, and archive contents

```bash
etc/bin/verify-distributions.sh v262.0.0 /tmp/grails-ij-verify
```

For both archives this verifies the `.sha512` checksum and the `.asc` detached signature
(imported into a throwaway GPG home, so a key you already trust locally cannot mask a bad
signature), then checks the contents:

- **Source distribution** must contain `LICENSE`, `NOTICE`, `README.md`, `INSTALL`,
  `RELEASE.md`, `.sdkmanrc`, and `gradle-bootstrap/`, and must **not** contain
  `gradle-wrapper.jar`, `gradlew`, `.git/`, `.github/`, or `.asf.yaml`.
- **Binary distribution** must carry `META-INF/LICENSE`, `META-INF/NOTICE`, and
  `META-INF/plugin.xml` inside the composed plugin jar.

The equivalent manual commands are:

```bash
shasum -a 512 -c apache-grails-intellij-plugin-<version>-src.zip.sha512
gpg --verify apache-grails-intellij-plugin-<version>-src.zip.asc \
             apache-grails-intellij-plugin-<version>-src.zip
```

### 6.3 Run the license audit

The Apache RAT audit runs against the **source distribution**, since that is the artifact
being released:

```bash
./gradlew rat
```

`verify.sh` extracts the source distribution, bootstraps its wrapper, and runs `rat` there.

### 6.4 Rebuild from source and compare to the staged binary

```bash
etc/bin/verify-reproducible.sh v262.0.0 /tmp/grails-ij-verify
```

This is the check that proves the published binary was built from the voted-on source. It
extracts the source distribution, bootstraps the Gradle Wrapper, rebuilds `buildPlugin`,
and compares the result against the staged binary.

> **The two ZIP files will never have the same SHA.** The staged binary is signed for the
> JetBrains Marketplace after the build, and a local rebuild is unsigned. What must match
> is everything the *build* produced, so the script compares the extracted archive
> contents entry by entry rather than hashing the ZIPs.

### 6.5 Manual checks

- Install the binary distribution into IntelliJ IDEA Ultimate via
  **Settings > Plugins > Install Plugin from Disk**, open a Grails project, and confirm
  the plugin loads and GSP support works.
- Confirm the signing key belongs to a Grails PMC member — see
  [Appendix: GPG & the KEYS file](#appendix-gpg--the-keys-file).

## 7. Vote

The release vote is held on the
[Grails dev mailing list](https://lists.apache.org/list.html?dev@grails.apache.org),
following the [Apache voting process](https://www.apache.org/foundation/voting.html).
Only Grails PMC votes are binding; the vote runs a minimum of 72 hours and needs at least
three +1 votes from PMC members.

**You do not need to write the email.** The `upload` job's final step,
*📧 Print Grails PMC Vote Email*, renders it for you and prints it to the job log with the
recipient, subject, and body ready to copy. It fills in the version, tag, tag commit id,
staged artifact URLs, and the `dist.apache.org` SVN revision the artifacts were committed
at, so voters can confirm they are looking at exactly what was staged.

The email bodies live in [`.github/vote_templates`](.github/vote_templates) and are
expanded with `envsubst`:

| Template | Used by | Sent as |
| --- | --- | --- |
| `staged.txt` | `upload` job | `[VOTE]` to `dev@grails.apache.org` |
| `vote_succeeded.txt` | `release` job | `[RESULT][VOTE]` to `dev@grails.apache.org` |
| `announce.txt` | `release` job | `[ANNOUNCE]` to `announce@apache.org`, `dev@`, `users@` |

Edit the templates rather than the workflow when the wording needs to change. Placeholders
in angle brackets (`<X>`, `<NAME>`, `<PREVIOUS_VERSION>`) are deliberately left for the
release manager to fill in; everything in `${...}` is substituted automatically.

## 8. Publish the release (`release` job)

After the vote succeeds, approve the `release` job (the `release` GitHub environment
requires the reviewers listed in `.asf.yaml`):

1. **MANUAL:** confirm the vote result on `dev@grails.apache.org`.
2. **MANUAL:** promote the staged artifacts from `dist/dev` to `dist/release`. ASF
   infrastructure requires this be done by a human under their own credentials, so it
   cannot be automated:
   ```bash
   .github/scripts/releaseDistributions.sh v<version> INTELLIJ <ASF_USER>
   ```
   The script also offers to remove prior release folders — ASF dist keeps only the
   current release, with older ones served from the archive.
3. `./gradlew publishPlugin -Pversion=${VERSION}` — publish the convenience binary to the
   JetBrains Marketplace.
4. **MANUAL:** record the release at <https://reporter.apache.org/addrelease.html?grails>
   as `INTELLIJ-<version>`, dated the day the distributions were promoted.
5. **MANUAL:** send the `[RESULT][VOTE]` and `[ANNOUNCE]` emails. The job's last two steps
   print both, rendered from
   [`.github/vote_templates`](.github/vote_templates) — copy them from the log and fill in
   the angle-bracket placeholders. Announcements must come from your `@apache.org` address
   (see <https://infra.apache.org/committer-email.html>).
6. Flag the GitHub Release as **latest**.

## Rollback

A release can be withdrawn cleanly **only before the vote passes**. Once artifacts are
promoted to `dist/release` and published to the Marketplace, they are public and the
correct response is a new version, not a rollback.

Run the **Release - Abort Release** workflow
([`.github/workflows/release-abort.yml`](.github/workflows/release-abort.yml)) from the
Actions tab, giving it the release tag and ticking the confirmation box. It will:

1. Refuse to continue if the version already exists under
   `dist/release/grails/INTELLIJ` — that means the vote passed and the release is public.
2. Remove the staged distributions from `dist/dev/grails/INTELLIJ/<version>`.
3. Cancel any in-flight `Release` workflow runs.
4. Delete the GitHub Release and the git tag.

Afterwards, if a `[VOTE]` thread was already started, reply to it with `[CANCEL][VOTE]`
and the reason. The workflow prints this reminder at the end.

The equivalent manual steps, if the workflow cannot be used:

```bash
svn rm -m "Abort Apache Grails IntelliJ Plugin <version>" \
  https://dist.apache.org/repos/dist/dev/grails/INTELLIJ/<version>
git push --delete origin v<version>
```

plus deleting the GitHub Release from the Releases page.

Nothing is published to the JetBrains Marketplace until the post-vote `release` job runs,
so aborting before that point leaves no public trace.

## Verifying a Reproducible Build

Reproducibility is enforced by the `grails-intellij.reproducible` convention plugin in
[`build-logic`](build-logic/src/main/kotlin/grails-intellij.reproducible.gradle.kts),
applied to the root project and every module. It pins archive entry timestamps
(`preserveFileTimestamps = false`), file order (`reproducibleFileOrder = true`), and entry
permissions, which removes the three usual sources of archive non-determinism.

### On the same machine

From a clean checkout of the tagged commit:

```bash
etc/bin/test-reproducible-build.sh
```

The script pins `SOURCE_DATE_EPOCH` to the last commit time, builds `buildPlugin` twice
from a clean tree (`--rerun-tasks --no-build-cache`), and compares the SHA-256 of the
plugin ZIP and every module jar. On success it prints `(none) ✅ build is reproducible`.

On failure it lists each differing artifact and leaves the evidence in `etc/bin/results`:
`first.txt` / `second.txt` hold the full hash listings, and `firstArtifact/` /
`secondArtifact/` hold each build's copy of the artifacts, so the two can be unzipped and
diffed to find what moved.

> ⚠️ The script runs `git clean -xdf` between builds, which deletes **all** untracked
> files — including the multi-GB `.intellijPlatform` download cache, so each build
> re-downloads the IntelliJ Platform. Run it in a throwaway checkout or after stashing
> your work. `etc/bin/results` is preserved across the cleans.

### In a container (build-environment parity)

To rule out host-OS differences, reproduce inside a container that resembles the CI Linux
environment — see
[Appendix: Verification from a container](#appendix-verification-from-a-container).

### Common reproducibility gotchas

- **Archive timestamps / order / permissions** — handled by the reproducible convention
  plugin. Any new module must apply it, or its jar will not be reproducible.
- **`SOURCE_DATE_EPOCH`** — most tools honour it for embedded dates. Archive entries do
  not depend on it here (timestamps are pinned to a constant), but the test script sets it
  from the last git commit so anything that *does* read it agrees between the two builds.
- **JDK version** — a different JDK can change bytecode. Always build with the JDK pinned
  in `.sdkmanrc`; the Docker image installs exactly that version. This is also why the
  build deliberately sets no Gradle toolchain: the pinned JDK is the contract.
- **Properties files** — `java.util.Properties#store` writes a timestamp comment. Do not
  ship generated `.properties` files without stripping it.
- **Reflection order** — methods and fields returned by reflection are not in a
  guaranteed order across platforms. Any generator that reflects over a class must sort
  its results.

## Appendix: GPG & the KEYS file

The plugin releases under the shared `grails` dist project, so it uses the project-wide
KEYS file rather than one of its own:

<https://dist.apache.org/repos/dist/release/grails/KEYS>

To verify artifacts manually, import it and trust the signing key:

```bash
wget https://dist.apache.org/repos/dist/release/grails/KEYS
gpg --import KEYS
```

```bash
gpg --edit-key <key id>
gpg> trust
gpg> 4
gpg> quit
```

The verification scripts in `etc/bin` import KEYS into a temporary GPG home instead, so
they neither modify nor depend on your keyring.

## Appendix: Verification from a container

The official artifacts are built on Linux in GitHub Actions. To reproduce that
environment locally, use [`etc/bin/Dockerfile`](etc/bin/Dockerfile), which is pinned to
the same Liberica JDK as `.sdkmanrc`:

```bash
docker build -t grails-ij:testing -f etc/bin/Dockerfile .
docker run -it --rm -v "$(pwd):/home/builder/project" grails-ij:testing bash
# inside the container:
etc/bin/verify.sh v262.0.0 /tmp/grails-ij-verify
```

Anyone bumping the JDK in `.sdkmanrc` must bump the `FROM` line in the Dockerfile to
match, or the container will no longer reproduce the released artifacts.

## Appendix: Release secrets

| Secret | Purpose |
| --- | --- |
| `GRAILS_GPG_KEY` | ASF GPG private key used to sign the source and binary distributions. |
| `GPG_KEY_ID` | ID of that key, passed as `--default-key`. Obtain with `gpg --list-keys --keyid-format short`. |
| `SVC_DIST_GRAILS_USERNAME` | SVN user for `dist.apache.org`. Infra grants write access to `dev` only; promotion to `release` is manual. |
| `SVC_DIST_GRAILS_PASSWORD` | Password for the above. |
| `CERTIFICATE_CHAIN` | JetBrains Marketplace signing certificate chain. |
| `PRIVATE_KEY` | JetBrains Marketplace signing private key. |
| `PRIVATE_KEY_PASSWORD` | Password for the above. |
| `PUBLISH_TOKEN` | JetBrains Marketplace publishing token. |

Multi-line secrets such as the GPG key must be entered **with a trailing newline**;
omitting it is a common cause of signing failures that only surface during a release.

## Appendix: Why the build must be reproducible

GitHub Actions is [untrusted hardware](https://www.apache.org/legal/release-policy.html#owned-controlled-hardware)
as far as ASF release policy is concerned. Projects that release from it must make the
build **verifiable**: anyone must be able to rebuild the artifacts elsewhere and show they
are equivalent to what CI produced. A reproducible build is how that is demonstrated, and
it is why [section 6.4](#64-rebuild-from-source-and-compare-to-the-staged-binary) is part
of the pre-vote checklist rather than an optional extra.

The tooling that supports this:

| Path | Role |
| --- | --- |
| `build-logic/src/main/kotlin/grails-intellij.reproducible.gradle.kts` | Pins archive timestamps, file order, and permissions across every module. |
| `etc/bin/test-reproducible-build.sh` | Builds twice on one machine and compares artifact hashes. |
| `etc/bin/verify-reproducible.sh` | Rebuilds from the staged source distribution and compares to the staged binary. |
| `etc/bin/Dockerfile` | Linux environment matching CI, for ruling out host-OS differences. |
| `.sdkmanrc` | The pinned JDK and Gradle versions; the single source of truth for both CI and the Dockerfile. |

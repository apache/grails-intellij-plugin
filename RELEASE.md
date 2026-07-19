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
reproducible from the tagged commit (see [Verifying a Reproducible Build](#verifying-a-reproducible-build)).

The pipeline is encoded in [`.github/workflows/release.yml`](.github/workflows/release.yml)
and is triggered when a **GitHub Release is published** for a tag (`vX.Y.Z`). It runs in
four stages.

## Prerequisites

- The release tag (`vX.Y.Z`) points at the exact commit to be released.
- Marketplace signing / publishing secrets are configured: `CERTIFICATE_CHAIN`,
  `PRIVATE_KEY`, `PRIVATE_KEY_PASSWORD`, `PUBLISH_TOKEN`.
- ASF signing + dist secrets are configured: `GRAILS_GPG_KEY`, `GPG_KEY_ID`,
  `SVC_DIST_GRAILS_USERNAME`, `SVC_DIST_GRAILS_PASSWORD`.
- The release environment reviewers are set in [`.asf.yaml`](.asf.yaml) (GitHub does not
  allow private groups as approvers, so approvers are listed individually).
- The JDK is pinned by [`.sdkmanrc`](.sdkmanrc); CI reads the version from it so local and
  CI builds match. See [INSTALL](INSTALL) for local build setup.

## 1. Build & stage the plugin ZIP (`publish` job)

1. Establish `VERSION` from the tag (`${TAG#v}`).
2. `./gradlew check buildPlugin -Pversion=${VERSION}` — full build, tests, and Plugin ZIP.
3. `./gradlew signPlugin` — JetBrains Marketplace signature.
4. Rename to `apache-grails-intellij-plugin-${VERSION}-bin.zip`.
5. GPG-detached-sign (`.asc`) and checksum (`.sha512`).
6. Upload the ZIP, `.asc`, and `.sha512` to the GitHub Release.

## 2. Create the source distribution (`source` job)

1. Check out the tag, then remove files that must not ship in a source release
   (`.git`, `.github`, `.asf.yaml`, `gradlew`/`gradlew.bat`, `gradle/wrapper`).
2. Zip to `apache-grails-intellij-plugin-${VERSION}-src.zip`.
3. GPG-detached-sign (`.asc`) and checksum (`.sha512`).
4. Upload all three to the GitHub Release.

## 3. Stage to dist.apache.org & start the vote (`upload` job)

1. Ensure `dist/dev/grails/INTELLIJ` exists in the ASF dist SVN.
2. Upload the source + binary distributions (with signatures and checksums) under
   `dist/dev/grails/INTELLIJ/${VERSION}/`.
3. **MANUAL:** start a `[VOTE]` thread for the release on `dev@grails.apache.org`.

Reviewers should verify signatures, checksums, the RAT license audit, and a
**reproducible build** before voting (next section).

## 4. Publish the release (`release` job — post-vote, environment-gated)

After the vote succeeds (the `release` GitHub environment requires the reviewers in
`.asf.yaml`):

1. **MANUAL:** confirm the vote result on `dev@grails.apache.org`.
2. **MANUAL:** promote the staged artifacts from `dist/dev` to `dist/release` via
   [`.github/scripts/releaseDistributions.sh`](.github/scripts/releaseDistributions.sh).
3. `./gradlew publishPlugin -Pversion=${VERSION}` — publish the convenience binary to the
   JetBrains Marketplace.
4. **MANUAL:** update the [ASF release reporter](https://reporter.apache.org/) and send
   the `[RESULT][VOTE]` + `[ANNOUNCE]` emails.

## Verifying a Reproducible Build

A vote is a good time to confirm the artifacts can be rebuilt bit-for-bit from the tag.
Reproducibility is enforced by the `grails-intellij.reproducible` convention plugin
(`build-logic/`), which pins archive timestamps, file order, and entry permissions across
every module and the plugin ZIP.

### On the same machine

From a clean checkout of the tagged commit:

```bash
etc/bin/test-reproducible-build.sh
```

The script pins `SOURCE_DATE_EPOCH` to the last commit time, builds `buildPlugin` twice
from a clean tree (`--rerun-tasks --no-build-cache`), and compares the SHA-256 of the
plugin ZIP and every module jar. It prints `((none) ✅ build is reproducible)` on success
and lists any differing artifacts on failure.

> ⚠️ The script runs `git clean -xdf` between builds, which deletes **all** untracked
> files. Run it in a throwaway checkout or after stashing your work.

### In a container (build-environment parity)

To rule out host-OS differences, reproduce inside a container that resembles the CI Linux
environment ([`etc/bin/Dockerfile`](etc/bin/Dockerfile), pinned to the same Liberica JDK
as `.sdkmanrc`):

```bash
docker build -t grails-ij:testing -f etc/bin/Dockerfile .
docker run -it --rm -v "$(pwd):/home/builder/project" grails-ij:testing bash
# inside the container:
etc/bin/test-reproducible-build.sh
```

### Common reproducibility gotchas

- **Archive timestamps / order** — handled by the reproducible convention plugin
  (`preserveFileTimestamps=false`, `reproducibleFileOrder=true`, fixed permissions).
- **`SOURCE_DATE_EPOCH`** — most tools honor it for embedded dates; the script sets it
  from the last git commit so successive builds agree.
- **JDK version** — a different JDK can change bytecode. Always build with the JDK pinned
  in `.sdkmanrc`; the Docker image installs exactly that version.

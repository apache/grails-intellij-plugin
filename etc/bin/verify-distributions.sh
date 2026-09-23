#!/usr/bin/env bash
#
#  Licensed to the Apache Software Foundation (ASF) under one
#  or more contributor license agreements.  See the NOTICE file
#  distributed with this work for additional information
#  regarding copyright ownership.  The ASF licenses this file
#  to you under the Apache License, Version 2.0 (the
#  "License"); you may not use this file except in compliance
#  with the License.  You may obtain a copy of the License at
#
#    https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing,
#  software distributed under the License is distributed on an
#  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
#  KIND, either express or implied.  See the License for the
#  specific language governing permissions and limitations
#  under the License.
#
# Verifies the downloaded Apache Grails IntelliJ Plugin release artifacts. For both the
# source distribution and the convenience binary this checks:
#   * the .sha512 checksum
#   * the .asc detached GPG signature, against the Grails KEYS file
#   * that the archive contains the files ASF policy requires (LICENSE, NOTICE, ...) at its
#     root, and that those two say what they must -- the Apache License 2.0, and a NOTICE
#     naming this product -- rather than merely existing under the right name
#   * that LICENSE and NOTICE are identical across both archives and the plugin jar
#   * that the source distribution does NOT ship the Gradle Wrapper jar
#
# Signatures are verified in a throwaway GPG home holding nothing but the Grails KEYS file,
# so the caller's keyring is left alone and a key already trusted locally cannot mask a bad
# signature. That also makes the signature check the provenance check: KEYS lives in the ASF
# release dist area, which only the PMC can write to, so a signature that verifies here was
# made by a key the PMC published. There is no separate "is the signer a PMC member" step.
#
# Run etc/bin/download-release-artifacts.sh first to populate the download location.
#
# Usage:
#   etc/bin/verify-distributions.sh <release-tag> [download-location]
#
set -euo pipefail

RELEASE_TAG="${1:-}"
DOWNLOAD_LOCATION="${2:-downloads}"

if [ -z "${RELEASE_TAG}" ]; then
  echo "Usage: $0 <release-tag> [download-location]" >&2
  exit 1
fi

VERSION="${RELEASE_TAG#v}"
DIST_NAME="apache-grails-intellij-plugin"
SRC_ZIP="${DIST_NAME}-${VERSION}-src.zip"
BIN_ZIP="${DIST_NAME}-${VERSION}-bin.zip"
# The directory the source zip unpacks to; the release workflow zips the checkout directory
# itself, so the top-level entry is the repository name rather than the distribution name.
SRC_EXTRACTED="grails-intellij-plugin"

cd "${DOWNLOAD_LOCATION}"

# Portable SHA-512 verification: GNU coreutils on Linux, `shasum` on macOS.
if command -v sha512sum &> /dev/null; then
  check_sha() { sha512sum -c "$1"; }
else
  check_sha() { shasum -a 512 -c "$1"; }
fi

if [ ! -f KEYS ]; then
  echo "❌ KEYS not found in $(pwd) — run etc/bin/download-release-artifacts.sh first" >&2
  exit 1
fi

GPG_HOME="$(mktemp -d)"
cleanup() { rm -rf "${GPG_HOME}"; }
trap cleanup EXIT
chmod 700 "${GPG_HOME}"

echo "==> Importing KEYS into a throwaway GPG home"
gpg --homedir "${GPG_HOME}" --batch --quiet --import KEYS
echo "✅ KEYS imported"

verify_archive() { # <zip file>
  local zip="$1"
  echo ""
  echo "==> Verifying ${zip}"

  if [ ! -f "${zip}" ]; then
    echo "❌ ${zip} not found in $(pwd)" >&2
    return 1
  fi

  check_sha "${zip}.sha512"
  echo "✅ checksum verified"

  gpg --homedir "${GPG_HOME}" --batch --verify "${zip}.asc" "${zip}"
  echo "✅ signature verified against the Grails KEYS file"
}

# Match a pattern against a listing WITHOUT a pipe. `printf ... | grep -q` looks equivalent
# but is not: grep -q exits at the first match, the writer is killed with SIGPIPE while the
# rest of the listing is still queued, and `set -o pipefail` then reports the pipeline as
# failed (141) even though the pattern matched. The failure depends on where in the listing
# the match falls -- entries near the top fail, entries near the bottom pass -- so it shows
# up as a required file being "missing" from an archive that plainly contains it, and,
# worse, as a forbidden file being silently accepted. A herestring has no pipe and no
# writer to kill.
listing_has() { # <listing> <pattern>
  grep -qE "$2" <<< "$1"
}

# Fails if any required path is absent from the archive listing.
require_entries() { # <zip file> <label> <entry>...
  local zip="$1" label="$2"
  shift 2
  local listing entry missing=0
  listing="$(unzip -Z1 "${zip}")"
  for entry in "$@"; do
    if ! listing_has "${listing}" "${entry}"; then
      echo "❌ ${label}: required entry matching '${entry}' is missing from ${zip}" >&2
      missing=1
    fi
  done
  [ "${missing}" -eq 0 ]
}

# Fails if any forbidden path is present in the archive listing.
forbid_entries() { # <zip file> <label> <entry>...
  local zip="$1" label="$2"
  shift 2
  local listing entry found=0
  listing="$(unzip -Z1 "${zip}")"
  for entry in "$@"; do
    if listing_has "${listing}" "${entry}"; then
      echo "❌ ${label}: forbidden entry matching '${entry}' is present in ${zip}" >&2
      found=1
    fi
  done
  [ "${found}" -eq 0 ]
}

# Read one entry out of a zip by its exact path. Addressing the entry exactly, rather than by
# the pattern require_entries matched, means a LICENSE somewhere else in the tree can never
# stand in for the one at the root that is being checked.
entry_text() { # <zip file> <entry>
  unzip -p "$1" "$2"
}

text_has() { # <text> <pattern>
  grep -qE "$2" <<< "$1"
}

# Presence is not the check ASF policy asks for -- an empty, truncated or placeholder LICENSE
# passes `unzip -Z1 | grep LICENSE` and fails a release vote. Match the landmarks of the
# Apache License 2.0 instead: its title, the version line, and the end of the terms. Landmarks
# rather than a hash of the whole file, so an editor's trailing-newline change is not reported
# as a licensing problem.
verify_license_text() { # <label> <text>
  local label="$1" text="$2" bad=0 pattern
  for pattern in \
    '^[[:space:]]*Apache License$' \
    '^[[:space:]]*Version 2\.0, January 2004$' \
    '^[[:space:]]*END OF TERMS AND CONDITIONS$'; do
    if ! text_has "${text}" "${pattern}"; then
      echo "❌ ${label}: LICENSE is not the Apache License 2.0 -- no line matching '${pattern}'" >&2
      bad=1
    fi
  done
  [ "${bad}" -eq 0 ]
}

# NOTICE has to name this product and carry the ASF copyright; a NOTICE copied from another
# project satisfies every structural check there is. The year is matched as a range or a single
# year so the check survives the annual roll-over without edits.
verify_notice_text() { # <label> <text>
  local label="$1" text="$2" bad=0 pattern
  for pattern in \
    '^Apache Grails IntelliJ Plugin$' \
    '^Copyright [0-9]{4}(-[0-9]{4})? The Apache Software Foundation$' \
    'This product includes software developed at'; do
    if ! text_has "${text}" "${pattern}"; then
      echo "❌ ${label}: NOTICE has no line matching '${pattern}'" >&2
      bad=1
    fi
  done
  [ "${bad}" -eq 0 ]
}

# When LICENSE or NOTICE points at a bundled third-party license -- grails-core writes these as
# "See licenses/LICENSE-MIT.txt for the full license terms." -- that file has to travel in the
# same archive. Neither file carries such a reference today, so this passes trivially; it is
# here so the first bundled dependency cannot ship with a dangling pointer, which is the one
# licensing defect that looks fine in every listing.
require_referenced_licenses() { # <label> <text> <root prefix> <listing>
  local label="$1" text="$2" prefix="$3" listing="$4" ref missing=0
  while IFS= read -r ref; do
    [ -z "${ref}" ] && continue
    if ! grep -qxF -- "${prefix}${ref}" <<< "${listing}"; then
      echo "❌ ${label}: references '${ref}' but '${prefix}${ref}' is not in the archive" >&2
      missing=1
    fi
  done <<< "$(grep -oE 'See [^[:space:]]+ for the full license terms' <<< "${text}" | awk '{print $2}' || true)"
  [ "${missing}" -eq 0 ]
}

# Checks the LICENSE and NOTICE at the root of one archive: right file, right contents, and
# every license they reference packaged alongside them.
#
# Each sub-check is run for its own exit status and the failures are counted, rather than
# leaning on `set -e` to abort partway through. Two reasons: every problem in the pair gets
# reported in one run instead of one per re-run, and errexit does NOT propagate into a
# function body when the function is called from a condition or the right-hand side of `&&`,
# so a caller written that way would turn all of this into a no-op that always passes.
verify_root_license_files() { # <zip file> <label> <root prefix>
  local zip="$1" label="$2" prefix="$3" listing license notice failures=0
  listing="$(unzip -Z1 "${zip}")"
  license="$(entry_text "${zip}" "${prefix}LICENSE")"
  notice="$(entry_text "${zip}" "${prefix}NOTICE")"
  verify_license_text "${label}" "${license}" || failures=$((failures + 1))
  verify_notice_text "${label}" "${notice}" || failures=$((failures + 1))
  require_referenced_licenses "${label} LICENSE" "${license}" "${prefix}" "${listing}" ||
    failures=$((failures + 1))
  require_referenced_licenses "${label} NOTICE" "${notice}" "${prefix}" "${listing}" ||
    failures=$((failures + 1))
  if [ "${failures}" -ne 0 ]; then
    echo "❌ ${label}: ${failures} of the LICENSE/NOTICE checks above failed" >&2
    return 1
  fi
  echo "✅ ${label}: LICENSE and NOTICE at the archive root carry the expected contents"
}

verify_archive "${SRC_ZIP}"
echo "==> Checking source distribution contents"
# Build and license instructions must be present so the source release is self-contained,
# and gradle-bootstrap must be there because the wrapper jar is deliberately absent.
#
# Anchor every pattern to the distribution root. An unanchored '/LICENSE$' is satisfied by a
# LICENSE anywhere in the tree -- a vendored one several directories down, say -- which is
# exactly the arrangement ASF policy forbids, so the loose pattern passes on the release that
# most needs to fail.
require_entries "${SRC_ZIP}" "source distribution" \
  "^${SRC_EXTRACTED}/LICENSE\$" \
  "^${SRC_EXTRACTED}/NOTICE\$" \
  "^${SRC_EXTRACTED}/README\.md\$" \
  "^${SRC_EXTRACTED}/INSTALL\$" \
  "^${SRC_EXTRACTED}/RELEASE\.md\$" \
  "^${SRC_EXTRACTED}/\.sdkmanrc\$" \
  "^${SRC_EXTRACTED}/gradle-bootstrap/build\.gradle\$"
# ASF source releases must not ship compiled binaries. No jar of any kind belongs here: the
# Gradle Wrapper jar is stripped by the release workflow, and the third-party test fixture jars
# are resolved from Maven at test time rather than committed, so a jar appearing in the source
# distribution means one of those two arrangements has regressed.
#
# .git / .github / .asf.yaml are infrastructure that is not part of the release, and the
# improvement plan is RAT-excluded on the grounds that it is removed before the first
# release, so nothing should have to audit it in a distribution.
forbid_entries "${SRC_ZIP}" "source distribution" \
  '\.jar$' \
  '/gradlew$' \
  '/gradlew\.bat$' \
  '/\.git/' \
  '/\.github/' \
  '/\.asf\.yaml$' \
  '/IMPROVEMENT-PLAN\.md$'
echo "✅ source distribution contents verified"
verify_root_license_files "${SRC_ZIP}" "source distribution" "${SRC_EXTRACTED}/"

verify_archive "${BIN_ZIP}"
echo "==> Checking binary distribution contents"
# ASF policy: the convenience binary must carry LICENSE and NOTICE, and the ZIP is the unit that
# gets staged, voted on and distributed -- policy applies "whether the unit of distribution is a
# .jar, .msi, .tar.gz, .zip, .exe installer, or any other file format". They sit at the root of
# the plugin directory (see PrepareSandboxTask in the intellij-plugin convention plugin).
require_entries "${BIN_ZIP}" "binary distribution" \
  '^[^/]*/LICENSE$' \
  '^[^/]*/NOTICE$'
# They are in the composed plugin jar's META-INF as well (see processResources in the same
# convention plugin), which is checked below.
#
# Identify that jar by name rather than by position: <plugin-dir>/lib/<plugin-dir>-<version>.jar.
# lib/ also holds the compiler and lib-tier jars, and picking the first entry would silently
# start inspecting one of those if the naming ever sorted differently.
# Take the first line by expansion rather than `| head -n 1`, which can SIGPIPE the writer
# ahead of it and trip pipefail the same way listing_has explains.
PLUGIN_DIRS="$(unzip -Z1 "${BIN_ZIP}" | sed -n 's|^\([^/]*\)/.*|\1|p' | sort -u)"
PLUGIN_DIR="${PLUGIN_DIRS%%$'\n'*}"
PLUGIN_JAR_MATCHES="$(unzip -Z1 "${BIN_ZIP}" |
  grep -E "^${PLUGIN_DIR}/lib/${PLUGIN_DIR}-[^/]+\.jar$" || true)"
PLUGIN_JAR_COUNT="$(printf '%s' "${PLUGIN_JAR_MATCHES}" | grep -c . || true)"
if [ "${PLUGIN_JAR_COUNT}" -ne 1 ]; then
  echo "❌ binary distribution: expected exactly 1 composed plugin jar at" \
       "${PLUGIN_DIR}/lib/${PLUGIN_DIR}-<version>.jar in ${BIN_ZIP}," \
       "found ${PLUGIN_JAR_COUNT}: ${PLUGIN_JAR_MATCHES}" >&2
  exit 1
fi
PLUGIN_JAR="${PLUGIN_JAR_MATCHES}"
JAR_TMP="$(mktemp -d)"
trap 'rm -rf "${GPG_HOME}" "${JAR_TMP}"' EXIT
unzip -q -o -j "${BIN_ZIP}" "${PLUGIN_JAR}" -d "${JAR_TMP}"
require_entries "${JAR_TMP}/$(basename "${PLUGIN_JAR}")" "binary distribution" \
  '^META-INF/LICENSE$' \
  '^META-INF/NOTICE$' \
  '^META-INF/plugin\.xml$'
echo "✅ binary distribution contents verified (${PLUGIN_JAR})"
verify_root_license_files "${BIN_ZIP}" "binary distribution" "${PLUGIN_DIR}/"

# Both archives, and the plugin jar's META-INF, are packaged from the one LICENSE and the one
# NOTICE at the repository root (see processResources and PrepareSandboxTask in the
# intellij-plugin convention plugin). Comparing them to each other turns that into something
# verifiable from the staged artifacts alone: the release cannot go out with the binary
# carrying a stale copy of a LICENSE that was updated for the source release, or vice versa.
echo "==> Checking LICENSE and NOTICE agree across the archives"
license_mismatch=0
for name in LICENSE NOTICE; do
  src_text="$(entry_text "${SRC_ZIP}" "${SRC_EXTRACTED}/${name}")"
  if [ "$(entry_text "${BIN_ZIP}" "${PLUGIN_DIR}/${name}")" != "${src_text}" ]; then
    echo "❌ ${name} at the root of ${BIN_ZIP} differs from the one in ${SRC_ZIP}" >&2
    license_mismatch=1
  fi
  if [ "$(unzip -p "${JAR_TMP}/$(basename "${PLUGIN_JAR}")" "META-INF/${name}")" != "${src_text}" ]; then
    echo "❌ META-INF/${name} in ${PLUGIN_JAR} differs from the ${name} in ${SRC_ZIP}" >&2
    license_mismatch=1
  fi
done
[ "${license_mismatch}" -eq 0 ]
echo "✅ LICENSE and NOTICE are identical in the source zip, the binary zip, and the plugin jar"

# Extract the source distribution next to the archives, the way grails-core's
# verify-source-distribution.sh does: the later steps (the RAT audit in verify.sh and the
# rebuild in verify-reproducible.sh) all work inside that one extracted tree, and its
# etc/bin/results is where the evidence ends up -- the same path it would occupy in a
# checkout, so it can be rsync'd straight back into the project.
echo ""
echo "==> Extracting the source distribution for the later verification steps"
rm -rf "${SRC_EXTRACTED}"
unzip -q "${SRC_ZIP}"
if [ ! -d "${SRC_EXTRACTED}" ]; then
  echo "❌ ${SRC_ZIP} did not extract to ${SRC_EXTRACTED}" >&2
  exit 1
fi
echo "✅ extracted to ${SRC_EXTRACTED}"

echo ""
echo "✅ All distribution checks passed for ${RELEASE_TAG}"

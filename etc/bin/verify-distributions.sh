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
#   * that the archive contains the files ASF policy requires (LICENSE, NOTICE, ...)
#   * that the source distribution does NOT ship the Gradle Wrapper jar
#
# Signatures are verified in a throwaway GPG home so the caller's keyring is left alone
# and so a key already trusted locally cannot mask a bad signature.
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
  echo "✅ signature verified"
}

# Fails if any required path is absent from the archive listing.
require_entries() { # <zip file> <label> <entry>...
  local zip="$1" label="$2"
  shift 2
  local listing entry missing=0
  listing="$(unzip -Z1 "${zip}")"
  for entry in "$@"; do
    if ! printf '%s\n' "${listing}" | grep -qE "${entry}"; then
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
    if printf '%s\n' "${listing}" | grep -qE "${entry}"; then
      echo "❌ ${label}: forbidden entry matching '${entry}' is present in ${zip}" >&2
      found=1
    fi
  done
  [ "${found}" -eq 0 ]
}

verify_archive "${SRC_ZIP}"
echo "==> Checking source distribution contents"
# Build and license instructions must be present so the source release is self-contained,
# and gradle-bootstrap must be there because the wrapper jar is deliberately absent.
require_entries "${SRC_ZIP}" "source distribution" \
  '/LICENSE$' \
  '/NOTICE$' \
  '/README\.md$' \
  '/INSTALL$' \
  '/RELEASE\.md$' \
  '/\.sdkmanrc$' \
  '/gradle-bootstrap/build\.gradle$'
# ASF source releases must not ship compiled binaries. No jar of any kind belongs here: the
# Gradle Wrapper jar is stripped by the release workflow, and the third-party test fixture jars
# are resolved from Maven at test time rather than committed, so a jar appearing in the source
# distribution means one of those two arrangements has regressed.
#
# .git / .github / .asf.yaml are infrastructure that is not part of the release, and the two
# planning documents are RAT-excluded on the grounds that they are removed before the first
# release, so nothing should have to audit them in a distribution.
forbid_entries "${SRC_ZIP}" "source distribution" \
  '\.jar$' \
  '/gradlew$' \
  '/gradlew\.bat$' \
  '/\.git/' \
  '/\.github/' \
  '/\.asf\.yaml$' \
  '/MIGRATION-PLAN\.md$' \
  '/IMPROVEMENT-PLAN\.md$'
echo "✅ source distribution contents verified"

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
PLUGIN_DIR="$(unzip -Z1 "${BIN_ZIP}" | sed -n 's|^\([^/]*\)/.*|\1|p' | sort -u | head -n 1)"
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

echo ""
echo "✅ All distribution checks passed for ${RELEASE_TAG}"

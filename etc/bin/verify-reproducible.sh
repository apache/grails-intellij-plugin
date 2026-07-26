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
# Rebuilds the plugin from the staged SOURCE distribution and compares the result with the
# staged convenience BINARY, which is what proves the published binary really was built
# from the voted-on source.
#
# Why this compares contents rather than the ZIP's SHA:
#
#   The staged binary is signed for the JetBrains Marketplace (`signPlugin`), and the
#   marketplace ZIP signer injects a signature into the archive after the build. A locally
#   rebuilt ZIP is unsigned, so the two ZIP files can never have the same hash even when
#   the build is perfectly reproducible. What must match is everything the build produced:
#   every entry inside the archive, byte for byte. That is what this script checks, and it
#   reports any entry that is present in only one side or whose contents differ.
#
# Requires a local Gradle on PATH (the source distribution ships without the wrapper jar,
# per ASF policy, so the wrapper has to be bootstrapped first). Install the version pinned
# in .sdkmanrc, e.g. with `sdk env install`.
#
# Run etc/bin/download-release-artifacts.sh first to populate the download location.
#
# Usage:
#   etc/bin/verify-reproducible.sh <release-tag> [download-location]
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
DOWNLOAD_DIR="$(pwd)"

for f in "${SRC_ZIP}" "${BIN_ZIP}"; do
  if [ ! -f "${f}" ]; then
    echo "❌ ${f} not found in ${DOWNLOAD_DIR} — run etc/bin/download-release-artifacts.sh first" >&2
    exit 1
  fi
done

if ! command -v gradle &> /dev/null; then
  echo "❌ no 'gradle' on PATH. The source distribution ships without the Gradle Wrapper jar," >&2
  echo "   so a local Gradle is needed to bootstrap it. Install the version pinned in" >&2
  echo "   .sdkmanrc (e.g. 'sdk env install') and re-run." >&2
  exit 1
fi

WORK="${DOWNLOAD_DIR}/reproducible"
rm -rf "${WORK}"
mkdir -p "${WORK}"

echo "==> Extracting source distribution"
unzip -q "${SRC_ZIP}" -d "${WORK}/src"
SRC_DIR="$(find "${WORK}/src" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
if [ -z "${SRC_DIR}" ]; then
  echo "❌ source distribution did not contain a top-level directory" >&2
  exit 1
fi
echo "✅ extracted to ${SRC_DIR}"

echo "==> Bootstrapping the Gradle Wrapper"
(cd "${SRC_DIR}/gradle-bootstrap" && gradle bootstrap)
echo "✅ wrapper bootstrapped"

echo "==> Rebuilding the plugin from source (this downloads the IntelliJ Platform; expect several GB)"
(cd "${SRC_DIR}" && ./gradlew buildPlugin -Pversion="${VERSION}" --no-daemon)

REBUILT_ZIP="$(find "${SRC_DIR}/plugin/build/distributions" -maxdepth 1 -type f -name '*.zip' | head -n 1)"
if [ -z "${REBUILT_ZIP}" ]; then
  echo "❌ rebuild produced no ZIP in ${SRC_DIR}/plugin/build/distributions" >&2
  exit 1
fi
echo "✅ rebuilt $(basename "${REBUILT_ZIP}")"

echo "==> Comparing rebuilt archive contents against the staged binary"
unzip -q "${REBUILT_ZIP}" -d "${WORK}/rebuilt"
unzip -q "${DOWNLOAD_DIR}/${BIN_ZIP}" -d "${WORK}/staged"

# Compare the extracted trees rather than the ZIPs themselves (see the header comment on
# marketplace signing). diff -r reports files that differ and files present on one side only.
if diff -r "${WORK}/rebuilt" "${WORK}/staged" > "${WORK}/diff.txt" 2>&1; then
  echo ""
  echo "✅ REPRODUCIBLE — every entry in the staged binary matches a rebuild from the staged source"
  exit 0
fi

echo ""
echo "❌ NOT REPRODUCIBLE — the staged binary does not match a rebuild from the staged source"
echo ""
echo "Differences:"
sed 's/^/  /' "${WORK}/diff.txt"
echo ""
echo "Full report: ${WORK}/diff.txt"
echo "Rebuilt tree: ${WORK}/rebuilt"
echo "Staged tree:  ${WORK}/staged"
echo ""
echo "Before concluding the release is bad, rule out environment differences: the official"
echo "artifacts are built on Linux with the JDK pinned in .sdkmanrc. Re-run this script in"
echo "the container built from etc/bin/Dockerfile, which matches that environment."
exit 1

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
# Layout mirrors grails-core: the work happens inside the extracted source distribution and
# the evidence lands in its etc/bin/results, the same path the results occupy in a checkout,
# so it can be rsync'd straight back into the project (see RELEASE.md).
#
#   <download-location>/grails-intellij-plugin/etc/bin/results/
#     first.txt        "<sha256>  <path>" for every file in the STAGED binary
#     second.txt       the same for the REBUILT binary
#     firstArtifact/   the staged plugin tree
#     secondArtifact/  the rebuilt plugin tree
#     diff.txt         the paths that differ (absent/empty when reproducible)
#
# first/second rather than staged/rebuilt keeps the names identical to
# test-reproducible-build.sh and to grails-core, where "first" is always the reference
# artifact and "second" the local rebuild.
#
# Why this compares contents rather than the ZIP's SHA:
#
#   The staged binary is signed for the JetBrains Marketplace (`signPlugin`), and the
#   marketplace ZIP signer injects a signature into the archive after the build. A locally
#   rebuilt ZIP is unsigned, so the two ZIP files can never have the same hash even when
#   the build is perfectly reproducible. What must match is everything the build produced:
#   every entry inside the archive, byte for byte.
#
# When a jar differs, the script opens it and reports which entries inside it differ, and
# for MANIFEST.MF which attributes -- the difference is usually one embedded build-
# environment value rather than anything in the bytecode, and "Binary files ... differ" on
# its own does not say that.
#
# Requires a local Gradle on PATH (the source distribution ships without the wrapper jar,
# per ASF policy, so the wrapper has to be bootstrapped first). Install the version pinned
# in .sdkmanrc, e.g. with `sdk env install`.
#
# Run etc/bin/download-release-artifacts.sh and etc/bin/verify-distributions.sh first: the
# former fetches the archives, the latter extracts the source distribution this rebuilds.
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
SRC_EXTRACTED="grails-intellij-plugin"

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

# Portable SHA-256, matching test-reproducible-build.sh: GNU coreutils on Linux, `shasum`
# on macOS. Both emit "<64 hex><2 spaces><path>", which the comparison below relies on.
if command -v sha256sum &> /dev/null; then
  sha() { sha256sum "$@"; }
else
  sha() { shasum -a 256 "$@"; }
fi

# verify-distributions.sh extracts the source distribution; do it here too so this script
# still works when run on its own.
SRC_DIR="${DOWNLOAD_DIR}/${SRC_EXTRACTED}"
if [ ! -d "${SRC_DIR}" ]; then
  echo "==> Extracting source distribution"
  unzip -q "${SRC_ZIP}"
  if [ ! -d "${SRC_DIR}" ]; then
    echo "❌ ${SRC_ZIP} did not extract to ${SRC_DIR}" >&2
    exit 1
  fi
fi
echo "✅ source distribution at ${SRC_DIR}"

RESULTS="${SRC_DIR}/etc/bin/results"
mkdir -p "${RESULTS}"

echo "==> Bootstrapping the Gradle Wrapper"
(cd "${SRC_DIR}/gradle-bootstrap" && gradle bootstrap)
echo "✅ wrapper bootstrapped"

echo "==> Rebuilding the plugin from source (this downloads the IntelliJ Platform; expect several GB)"
# --rerun-tasks --no-build-cache for the same reason test-reproducible-build.sh and
# grails-core's verify-reproducible.sh use them: the RAT audit in verify.sh already ran
# Gradle in this tree, and a reproducibility check must not be answered from task output
# or a cache entry produced by an earlier run.
(cd "${SRC_DIR}" && ./gradlew buildPlugin -Pversion="${VERSION}" \
    --rerun-tasks --no-build-cache --no-daemon)

REBUILT_ZIPS="$(find "${SRC_DIR}/plugin/build/distributions" -maxdepth 1 -type f -name '*.zip')"
REBUILT_ZIP="${REBUILT_ZIPS%%$'\n'*}"
if [ -z "${REBUILT_ZIP}" ]; then
  echo "❌ rebuild produced no ZIP in ${SRC_DIR}/plugin/build/distributions" >&2
  exit 1
fi
echo "✅ rebuilt $(basename "${REBUILT_ZIP}")"

echo "==> Unpacking both archives under ${RESULTS}"
rm -rf "${RESULTS}/firstArtifact" "${RESULTS}/secondArtifact"
mkdir -p "${RESULTS}/firstArtifact" "${RESULTS}/secondArtifact"
unzip -q "${DOWNLOAD_DIR}/${BIN_ZIP}" -d "${RESULTS}/firstArtifact"
unzip -q "${REBUILT_ZIP}" -d "${RESULTS}/secondArtifact"

# "<sha256>  <path>" with paths relative to the unpacked tree, so the two listings are
# directly comparable and readable on their own.
hash_tree() { # <directory> <listing file>
  local dir="$1" listing="$2"
  : > "${listing}"
  (cd "${dir}" && find . -type f | sed 's|^\./||' | sort | while IFS= read -r f; do
    sha "${f}"
  done) >> "${listing}"
}

echo "==> Hashing both trees"
hash_tree "${RESULTS}/firstArtifact" "${RESULTS}/first.txt"
hash_tree "${RESULTS}/secondArtifact" "${RESULTS}/second.txt"
echo "✅ $(wc -l < "${RESULTS}/first.txt" | tr -d ' ') staged files, $(wc -l < "${RESULTS}/second.txt" | tr -d ' ') rebuilt files"

# Compare by path rather than by whole line so a file present on only one side is reported
# as such instead of looking like two unrelated hash mismatches.
hash_of() { # <listing file> <path>
  awk -v p="$2" 'substr($0, 67) == p { print substr($0, 1, 64) }' "$1"
}

: > "${RESULTS}/diff.txt"
while IFS= read -r path; do
  [ -n "${path}" ] || continue
  first_hash="$(hash_of "${RESULTS}/first.txt" "${path}")"
  second_hash="$(hash_of "${RESULTS}/second.txt" "${path}")"
  if [ "${first_hash}" != "${second_hash}" ]; then
    printf '%s\n' "${path}" >> "${RESULTS}/diff.txt"
  fi
done < <(cut -c 67- "${RESULTS}/first.txt" "${RESULTS}/second.txt" | sort -u)

if [ ! -s "${RESULTS}/diff.txt" ]; then
  echo ""
  echo "✅ REPRODUCIBLE — every file in the staged binary matches a rebuild from the staged source"
  echo "   listings: ${RESULTS}/first.txt, ${RESULTS}/second.txt"
  exit 0
fi

echo ""
echo "❌ NOT REPRODUCIBLE — $(wc -l < "${RESULTS}/diff.txt" | tr -d ' ') file(s) differ between the staged binary and the rebuild"
echo ""

# Drill into each differing archive. A jar that differs in nothing but an embedded build
# value is a very different finding from one whose classes differ, and only this level of
# detail distinguishes them.
DRILL="${RESULTS}/entries"
rm -rf "${DRILL}"
while IFS= read -r path; do
  [ -n "${path}" ] || continue
  echo "---- ${path}"
  first_file="${RESULTS}/firstArtifact/${path}"
  second_file="${RESULTS}/secondArtifact/${path}"
  if [ ! -f "${first_file}" ]; then
    echo "     present only in the rebuild"
    continue
  fi
  if [ ! -f "${second_file}" ]; then
    echo "     present only in the staged binary"
    continue
  fi
  case "${path}" in
    *.jar|*.zip)
      rm -rf "${DRILL}/first" "${DRILL}/second"
      mkdir -p "${DRILL}/first" "${DRILL}/second"
      unzip -q -o "${first_file}" -d "${DRILL}/first"
      unzip -q -o "${second_file}" -d "${DRILL}/second"
      differing_entries="$(cd "${DRILL}/first" && find . -type f | sed 's|^\./||' | sort | while IFS= read -r entry; do
        cmp -s "${entry}" "${DRILL}/second/${entry}" || printf '%s\n' "${entry}"
      done)"
      if [ -z "${differing_entries}" ]; then
        echo "     contents identical; the archives differ only in entry metadata"
        echo "     (compare with: unzip -lv \"${first_file}\")"
      else
        printf '     differing entries: %s\n' "$(printf '%s' "${differing_entries}" | tr '\n' ' ')"
        # MANIFEST.MF is the usual culprit and its diff is short and readable, so show it.
        # grep on a herestring, not `printf | grep -q`, for the reason listing_has documents
        # in verify-distributions.sh. `|| true` because diff exits 1 when the files differ,
        # which is the expected case here and must not abort the report under `set -e`.
        if grep -qx 'META-INF/MANIFEST.MF' <<< "${differing_entries}"; then
          echo "     MANIFEST.MF attributes that differ (staged | rebuilt):"
          { diff "${DRILL}/first/META-INF/MANIFEST.MF" "${DRILL}/second/META-INF/MANIFEST.MF" ||
            true; } | sed -n 's/^[<>] /       /p'
        fi
      fi
      ;;
    *)
      echo "     files differ (not an archive)"
      ;;
  esac
done < "${RESULTS}/diff.txt"
rm -rf "${DRILL}"

echo ""
echo "Evidence under ${RESULTS}:"
echo "  diff.txt        the differing paths"
echo "  first.txt       hashes of the staged binary's files"
echo "  second.txt      hashes of the rebuilt binary's files"
echo "  firstArtifact/  the staged plugin tree"
echo "  secondArtifact/ the rebuilt plugin tree"
echo ""
echo "Copy it back into a checkout to work on it with:"
echo "  rsync -av ${RESULTS}/ <project>/etc/bin/results/"
echo ""
echo "Before concluding the release is bad, rule out environment differences: the official"
echo "artifacts are built on Linux with the JDK pinned in .sdkmanrc. Re-run this script in"
echo "the container built from etc/bin/Dockerfile, which matches that environment."
exit 1

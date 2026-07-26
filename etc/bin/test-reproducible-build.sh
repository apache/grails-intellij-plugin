#!/usr/bin/env bash
#
#  Licensed to the Apache Software Foundation (ASF) under one or more
#  contributor license agreements.  See the NOTICE file distributed with
#  this work for additional information regarding copyright ownership.
#  The ASF licenses this file to You under the Apache License, Version 2.0
#  (the "License"); you may not use this file except in compliance with
#  the License.  You may obtain a copy of the License at
#
#      https://www.apache.org/licenses/LICENSE-2.0
#
#  Unless required by applicable law or agreed to in writing, software
#  distributed under the License is distributed on an "AS IS" BASIS,
#  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
#  See the License for the specific language governing permissions and
#  limitations under the License.
#
# Verifies that the Grails IntelliJ plugin builds reproducibly: it builds the plugin
# ZIP (and every module jar) twice from a clean tree with SOURCE_DATE_EPOCH pinned, then
# compares the SHA-256 of each produced artifact. Any artifact whose hash differs between
# the two builds is printed and the script exits non-zero.
#
# Output is written to etc/bin/results (git-ignored):
#   first.txt / second.txt  - the full "<sha256>  <path>" listing from each build
#   firstArtifact/          - build 1's copy of every artifact
#   secondArtifact/         - build 2's copy of every artifact
# When a run fails, unzip the two copies of a differing artifact and diff them to find
# what moved (a timestamp, an entry order, a permission bit, an embedded build date).
#
# WARNING: this runs `git clean -xdf`, which deletes ALL untracked files (build output,
# .idea, IDE caches, and the several GB .intellijPlatform download cache), so each build
# re-downloads the IntelliJ Platform. Run it only in a throwaway checkout or after
# stashing work. etc/bin/results is preserved across the cleans.
#
# Usage: run from anywhere; the script locates the project root relative to itself.
#   etc/bin/test-reproducible-build.sh
set -euo pipefail

export SOURCE_DATE_EPOCH="$(git log -1 --pretty=%ct)"

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"
PROJECT_ROOT="$(cd -- "${SCRIPT_DIR}/../.." &> /dev/null && pwd)"
cd "${PROJECT_ROOT}"

# Portable SHA-256: prefer GNU coreutils, fall back to macOS `shasum`.
if command -v sha256sum &> /dev/null; then
  sha() { sha256sum "$@"; }
else
  sha() { shasum -a 256 "$@"; }
fi

RESULTS="etc/bin/results"

# List the release artifacts (plugin ZIP) and every module jar, one relative path per
# line, sorted so the output is order-stable across runs.
list_artifacts() {
  {
    find plugin/build/distributions -type f -name '*.zip' 2> /dev/null || true
    find . -type f -path '*/build/libs/*.jar' 2> /dev/null || true
  } | sed 's|^\./||' | sort
}

# Write "<sha256>  <path>" for every artifact, and stash a copy of each one under
# $RESULTS/$1 so a failing run can be diffed after the next `git clean` wipes the tree.
snapshot_artifacts() {
  local dest="${RESULTS}/$1" listing="${RESULTS}/$2" f
  rm -rf "${dest}"
  mkdir -p "${dest}"
  : > "${listing}"
  while IFS= read -r f; do
    [ -n "$f" ] || continue
    sha "$f" >> "${listing}"
    mkdir -p "${dest}/$(dirname -- "$f")"
    cp -- "$f" "${dest}/$f"
  done < <(list_artifacts)
}

build_once() {
  # -e keeps the results directory: it holds the previous build's artifacts and listings,
  # which are the only evidence left once the tree is wiped.
  git clean -xdf -e "${RESULTS}"
  ./gradlew buildPlugin --rerun-tasks --no-build-cache --no-daemon
}

mkdir -p "${RESULTS}"

echo "==> First build (SOURCE_DATE_EPOCH=${SOURCE_DATE_EPOCH})"
build_once
snapshot_artifacts firstArtifact first.txt

echo "==> Second build"
build_once
snapshot_artifacts secondArtifact second.txt

echo ""
echo "Comparing $(wc -l < "${RESULTS}/first.txt" | tr -d ' ') artifacts:"

# Compare by path rather than by whole line, so an artifact that exists in only one build
# is reported as such instead of silently looking like two unrelated hash mismatches.
# Both sha256sum and `shasum -a 256` emit a fixed "<64 hex><2 spaces><path>" layout, so
# the hash is columns 1-64 and the path starts at column 67 even if it contains spaces.
hash_of() { # <listing file> <path>
  awk -v p="$2" 'substr($0, 67) == p { print substr($0, 1, 64) }' "$1"
}

DIFF=""
while IFS= read -r path; do
  [ -n "$path" ] || continue
  first_hash="$(hash_of "${RESULTS}/first.txt" "$path")"
  second_hash="$(hash_of "${RESULTS}/second.txt" "$path")"
  if [ "${first_hash}" != "${second_hash}" ]; then
    DIFF="${DIFF}${path} (build1=${first_hash:-<missing>} build2=${second_hash:-<missing>})"$'\n'
  fi
done < <(cut -c 67- "${RESULTS}/first.txt" "${RESULTS}/second.txt" | sort -u)

if [ -z "$DIFF" ]; then
  echo "  (none) ✅ build is reproducible"
  exit 0
else
  printf '  %s\n' "${DIFF%$'\n'}"
  echo ""
  echo "❌ build is NOT reproducible"
  echo "   full hash lists: ${RESULTS}/first.txt, ${RESULTS}/second.txt"
  echo "   artifact copies: ${RESULTS}/firstArtifact/, ${RESULTS}/secondArtifact/"
  echo "   to find the cause, unzip both copies of a differing artifact and diff them:"
  echo "     unzip -o ${RESULTS}/firstArtifact/<path> -d ${RESULTS}/x1"
  echo "     unzip -o ${RESULTS}/secondArtifact/<path> -d ${RESULTS}/x2"
  echo "     diff -r ${RESULTS}/x1 ${RESULTS}/x2"
  echo "   if the contents match, compare entry metadata instead:"
  echo "     unzip -lv ${RESULTS}/firstArtifact/<path>"
  exit 1
fi

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
# WARNING: this runs `git clean -xdf`, which deletes ALL untracked files (build output,
# .idea, IDE caches, etc.). Run it only in a throwaway checkout or after stashing work.
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

# Hash the release artifacts (plugin ZIP) and every module jar, sorted by path so the
# output is order-stable across runs.
hash_artifacts() {
  {
    find build/distributions -type f -name '*.zip' 2> /dev/null || true
    find . -type f -path '*/build/libs/*.jar' 2> /dev/null || true
  } | sort | while IFS= read -r f; do
    [ -f "$f" ] && sha "$f"
  done
}

build_once() {
  git clean -xdf
  ./gradlew buildPlugin --rerun-tasks --no-build-cache --no-daemon
}

echo "==> First build (SOURCE_DATE_EPOCH=${SOURCE_DATE_EPOCH})"
build_once
FIRST="$(hash_artifacts)"

echo "==> Second build"
build_once
SECOND="$(hash_artifacts)"

printf '%s\n' "$FIRST" > first.txt
printf '%s\n' "$SECOND" > second.txt

echo ""
echo "Differing artifacts:"
# comm on the second (hash) field: lines present in only one build indicate a hash mismatch.
DIFF="$(comm -3 <(printf '%s\n' "$FIRST" | sort) <(printf '%s\n' "$SECOND" | sort) \
  | awk '{print $2}' | sed 's|^[[:space:]]*||' | grep -v '^$' | sort -u || true)"

if [ -z "$DIFF" ]; then
  echo "  (none) ✅ build is reproducible"
  exit 0
else
  printf '  %s\n' "$DIFF"
  echo ""
  echo "❌ build is NOT reproducible — see first.txt / second.txt for the full hash lists"
  exit 1
fi

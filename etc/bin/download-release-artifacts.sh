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
# Downloads the staged Apache Grails IntelliJ Plugin release artifacts (source
# distribution and convenience binary, each with its .asc signature and .sha512
# checksum) from the ASF dev distribution area, plus the Grails KEYS file used to
# verify the signatures.
#
# Usage:
#   etc/bin/download-release-artifacts.sh <release-tag> [download-location]
#
# Example:
#   etc/bin/download-release-artifacts.sh v262.0.0 /tmp/grails-ij-verify
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
# The plugin releases under the shared `grails` dist project, in its own INTELLIJ folder.
DEV_ROOT="https://dist.apache.org/repos/dist/dev/grails/INTELLIJ/${VERSION}"
KEYS_URL="https://dist.apache.org/repos/dist/release/grails/KEYS"

mkdir -p "${DOWNLOAD_LOCATION}"
cd "${DOWNLOAD_LOCATION}"

echo "==> Downloading KEYS"
curl -f -L -o KEYS "${KEYS_URL}"
echo "✅ KEYS downloaded"

# The release workflow stages the source distribution under sources/ and the
# convenience binary under distribution/; keep that split so paths match dist.apache.org.
echo "==> Downloading source distribution from ${DEV_ROOT}/sources"
for ext in "" ".asc" ".sha512"; do
  curl -f -L -O "${DEV_ROOT}/sources/${DIST_NAME}-${VERSION}-src.zip${ext}"
done
echo "✅ Source distribution downloaded"

echo "==> Downloading binary distribution from ${DEV_ROOT}/distribution"
for ext in "" ".asc" ".sha512"; do
  curl -f -L -O "${DEV_ROOT}/distribution/${DIST_NAME}-${VERSION}-bin.zip${ext}"
done
echo "✅ Binary distribution downloaded"

echo ""
echo "Artifacts in $(pwd):"
ls -l

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
# Runs the full pre-vote verification of a staged Apache Grails IntelliJ Plugin release:
#
#   1. download   - fetch the staged artifacts and the Grails KEYS file
#   2. distributions - checksums, GPG signatures, and required/forbidden archive contents
#   3. license    - Apache RAT audit, run from the extracted source distribution
#   4. reproducible - rebuild from the source distribution and compare to the staged binary
#
# Step 4 is slow (it downloads the IntelliJ Platform and does a full build) and is the one
# most sensitive to host-OS differences. Pass --skip-reproducible to stop after step 3, or
# run the whole thing inside the container from etc/bin/Dockerfile for an environment that
# matches CI. See RELEASE.md for the full verification guide.
#
# Usage:
#   etc/bin/verify.sh <release-tag> [download-location] [--skip-reproducible]
#
# Example:
#   etc/bin/verify.sh v262.0.0 /tmp/grails-ij-verify
#
set -euo pipefail

SCRIPT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" &> /dev/null && pwd)"

RELEASE_TAG=""
DOWNLOAD_LOCATION="downloads"
SKIP_REPRODUCIBLE=0

for arg in "$@"; do
  case "${arg}" in
    --skip-reproducible) SKIP_REPRODUCIBLE=1 ;;
    -*)
      echo "Unknown option: ${arg}" >&2
      exit 1
      ;;
    *)
      if [ -z "${RELEASE_TAG}" ]; then
        RELEASE_TAG="${arg}"
      else
        DOWNLOAD_LOCATION="${arg}"
      fi
      ;;
  esac
done

if [ -z "${RELEASE_TAG}" ]; then
  echo "Usage: $0 <release-tag> [download-location] [--skip-reproducible]" >&2
  exit 1
fi

VERSION="${RELEASE_TAG#v}"

mkdir -p "${DOWNLOAD_LOCATION}"
DOWNLOAD_LOCATION="$(cd "${DOWNLOAD_LOCATION}" && pwd)"

echo "############################################################"
echo "# Verifying Apache Grails IntelliJ Plugin ${VERSION}"
echo "# Artifacts: ${DOWNLOAD_LOCATION}"
echo "############################################################"

echo ""
echo "### 1/4 Downloading staged artifacts"
"${SCRIPT_DIR}/download-release-artifacts.sh" "${RELEASE_TAG}" "${DOWNLOAD_LOCATION}"

echo ""
echo "### 2/4 Verifying checksums, signatures, and archive contents"
"${SCRIPT_DIR}/verify-distributions.sh" "${RELEASE_TAG}" "${DOWNLOAD_LOCATION}"

echo ""
echo "### 3/4 Running the Apache RAT license audit on the source distribution"
# Audit the source distribution itself, not the local checkout: the release is the source
# archive, so that is what has to pass the license audit.
RAT_DIR="${DOWNLOAD_LOCATION}/rat"
rm -rf "${RAT_DIR}"
mkdir -p "${RAT_DIR}"
unzip -q "${DOWNLOAD_LOCATION}/apache-grails-intellij-plugin-${VERSION}-src.zip" -d "${RAT_DIR}"
RAT_SRC="$(find "${RAT_DIR}" -mindepth 1 -maxdepth 1 -type d | head -n 1)"
if command -v gradle &> /dev/null; then
  (cd "${RAT_SRC}/gradle-bootstrap" && gradle bootstrap)
  (cd "${RAT_SRC}" && ./gradlew rat --no-daemon)
  echo "✅ RAT audit passed"
else
  echo "⚠️  skipped: no 'gradle' on PATH to bootstrap the wrapper in the source distribution."
  echo "   Install the version pinned in .sdkmanrc (e.g. 'sdk env install') to run this step."
fi

echo ""
if [ "${SKIP_REPRODUCIBLE}" -eq 1 ]; then
  echo "### 4/4 Reproducible build check SKIPPED (--skip-reproducible)"
else
  echo "### 4/4 Rebuilding from source and comparing to the staged binary"
  "${SCRIPT_DIR}/verify-reproducible.sh" "${RELEASE_TAG}" "${DOWNLOAD_LOCATION}"
fi

echo ""
echo "############################################################"
echo "# ✅ Verification complete for ${RELEASE_TAG}"
echo "#"
echo "# Still to check by hand before voting:"
echo "#   * install ${DOWNLOAD_LOCATION}/apache-grails-intellij-plugin-${VERSION}-bin.zip"
echo "#     into IntelliJ IDEA Ultimate via Settings > Plugins > Install Plugin from Disk,"
echo "#     open a Grails project, and confirm the plugin loads and GSP support works"
echo "#   * confirm the GPG key that signed the artifacts belongs to a Grails PMC member"
echo "#     (see the KEYS appendix in RELEASE.md)"
echo "############################################################"

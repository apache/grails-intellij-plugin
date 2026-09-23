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

# Fail fast on anything the verification scripts depend on but do not install themselves,
# so a misconfigured environment is reported up front instead of halfway through a long
# run. gradle is required, not optional: the RAT audit and the reproducible rebuild both
# bootstrap the source distribution's wrapper with it, and a run that silently skips them
# is a green result that verified less than it claims to have.
preflight() {
  local missing=0 tool
  # gpg-agent is listed separately from gpg: GnuPG 2.x shells out to it even for an
  # --import into a throwaway homedir, and Debian's gpg package only recommends it.
  for tool in java gpg gpg-agent curl unzip gradle; do
    if ! command -v "${tool}" > /dev/null 2>&1; then
      echo "❌ Required tool not found on \$PATH: ${tool}"
      missing=1
    fi
  done
  if [ "${missing}" -ne 0 ]; then
    echo "❌ Preflight checks failed. Resolve the issues above before running verification."
    echo "   java/gradle: install the versions pinned in .sdkmanrc (\`sdk env install\`)."
    echo "   gpg/gpg-agent: from your package manager -- Debian installs the agent with the"
    echo "   gnupg package, not the smaller gpg package; macOS has it in \`brew install gnupg\`."
    echo "   In the container from etc/bin/Dockerfile everything above is preinstalled, so a"
    echo "   failure there means PATH was rebuilt by a login shell -- see RELEASE.md."
    exit 1
  fi
}

echo ""
echo "### Preflight"
preflight
echo "✅ java:   $(java -version 2>&1 | head -n 1)"
echo "✅ gradle: $(gradle --version | sed -n 's/^Gradle //p' | head -n 1)"
echo "✅ gpg:    $(gpg --version | head -n 1)"

echo ""
echo "### 1/4 Downloading staged artifacts"
"${SCRIPT_DIR}/download-release-artifacts.sh" "${RELEASE_TAG}" "${DOWNLOAD_LOCATION}"

echo ""
echo "### 2/4 Verifying checksums, signatures, and archive contents"
"${SCRIPT_DIR}/verify-distributions.sh" "${RELEASE_TAG}" "${DOWNLOAD_LOCATION}"

echo ""
echo "### 3/4 Running the Apache RAT license audit on the source distribution"
# Audit the source distribution itself, not the local checkout: the release is the source
# archive, so that is what has to pass the license audit. The tree is the one
# verify-distributions.sh extracted -- a single extraction shared by every later step, as
# in grails-core, rather than a second private copy per script.
RAT_SRC="${DOWNLOAD_LOCATION}/grails-intellij-plugin"
if [ ! -d "${RAT_SRC}" ]; then
  echo "❌ ${RAT_SRC} not found — verify-distributions.sh extracts it; run it first" >&2
  exit 1
fi
# Audit the archive exactly as staged, with the Gradle on PATH, and do NOT bootstrap the
# wrapper first: `gradle bootstrap` runs the `wrapper` task inside gradle-bootstrap/ and
# copies the result to the root, so it leaves behind a gradle-wrapper.properties that was
# never in the distribution. Auditing after that step reports a file the release does not
# contain. The bootstrap path is not skipped overall -- verify-reproducible.sh bootstraps
# this same tree and rebuilds from it in step 4.
(cd "${RAT_SRC}" && gradle rat --no-daemon)
echo "✅ RAT audit passed"

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

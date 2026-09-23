# Licensed to the Apache Software Foundation (ASF) under one or more
# contributor license agreements.  See the NOTICE file distributed with
# this work for additional information regarding copyright ownership.
# The ASF licenses this file to You under the Apache License, Version 2.0
# (the "License"); you may not use this file except in compliance with
# the License.  You may obtain a copy of the License at
#
#     https://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.

# Shell profile for the verification container built from etc/bin/Dockerfile. Installed as
# /etc/profile.d/grails-ij.sh and sourced from the groovy user's .bashrc, so it runs both
# for login shells (`docker exec -l`) and for the interactive bash the documented
# `docker run` lands in.
#
# It does two things:
#   1. restores PATH entries that /etc/profile drops -- login shells rebuild PATH from
#      scratch, losing both the JDK from the base image and the baked-in scripts
#      directory, which makes verify.sh fail its preflight for no visible reason;
#   2. on an interactive shell, prints the verification commands and whether the project
#      is mounted, so the container says what it is for instead of leaving the operator to
#      guess from an empty prompt.

SCRIPTS_DIR=/home/groovy/scripts
PROJECT_DIR=/home/groovy/project
VERIFY_DIR=/home/groovy/grails-verify

prepend_path() { # <directory>
    [ -d "$1" ] || return 0
    case ":${PATH}:" in
        *":$1:"*) ;;
        *) PATH="$1:${PATH}"; export PATH ;;
    esac
}

# JAVA_HOME is set by the base image; its bin directory is what /etc/profile discards.
[ -n "${JAVA_HOME:-}" ] && prepend_path "${JAVA_HOME}/bin"
prepend_path "${SCRIPTS_DIR}/etc/bin"
prepend_path "${SCRIPTS_DIR}"

# Interactive shells only -- never print a banner into a script's output.
case $- in
    *i*) ;;
    *) return 0 2>/dev/null || exit 0 ;;
esac

echo "Apache Grails IntelliJ Plugin -- release verification container"
echo "  JDK:    $(java -version 2>&1 | head -n 1)"
echo "  Gradle: $(gradle --version 2>/dev/null | sed -n 's/^Gradle //p' | head -n 1)"
echo ""
echo "Verification scripts are on PATH from ${SCRIPTS_DIR}/etc/bin (see RELEASE.md section 6):"
echo "  cd ${VERIFY_DIR} && verify.sh v<version> ."
echo "      download, checksum, signature, RAT and reproducible-rebuild checks"
echo "  cd ${VERIFY_DIR} && verify.sh v<version> . --skip-reproducible"
echo "      the same, without the slow rebuild"
echo ""

if [ -e "${PROJECT_DIR}/etc/bin/verify.sh" ]; then
    echo "Project mounted at ${PROJECT_DIR}:"
    echo "  cd ${PROJECT_DIR} && etc/bin/test-reproducible-build.sh"
    echo "      build this checkout twice and compare artifact hashes"
    echo "      (runs 'git clean -xdf' between builds -- throwaway checkouts only)"
    echo "  cd ${PROJECT_DIR} && etc/bin/verify.sh ..."
    echo "      runs the checkout's scripts instead of the image's baked-in copies"
else
    echo "No project mounted at ${PROJECT_DIR}. The verification scripts above still work;"
    echo "mount the checkout to run test-reproducible-build.sh or to copy results back:"
    echo ""
    echo '  docker run -it --rm -v "$(pwd):/home/groovy/project" grails-ij:testing bash'
    for candidate in /home/*/project /project /workspace /src; do
        if [ "${candidate}" != "${PROJECT_DIR}" ] && [ -e "${candidate}/etc/bin/verify.sh" ]; then
            echo ""
            echo "  (a checkout is mounted at ${candidate} -- this image expects ${PROJECT_DIR})"
        fi
    done
fi
echo ""

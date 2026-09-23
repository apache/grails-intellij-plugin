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

# For testing in a container that is similar to the Grails GitHub Actions Linux build
# environment, so reproducible-build differences can be attributed to the code rather than
# to the host OS. The layout deliberately matches grails-core's etc/bin/Dockerfile -- same
# user, same paths, same baked-in scripts -- so one habit works across the Grails repos.
#
# Run this from the root of the project:
#   docker build -t grails-ij:testing -f etc/bin/Dockerfile . && \
#     docker run -it --rm -v $(pwd):/home/groovy/project grails-ij:testing bash
#
# Then, inside the container:
#   cd grails-verify
#   verify.sh <git-tag-of-release> .
#
# The verification scripts are baked into the image at /home/groovy/scripts/etc/bin and put
# on PATH, so they are callable by bare name and do not depend on the bind mount. The mount
# is for the project itself: running test-reproducible-build.sh against the checkout, and
# copying results back to the host. Because the scripts are baked in, an edit to etc/bin
# needs a rebuild -- which is why the documented command chains build && run.
#
# Keep the java & gradle versions synced with .sdkmanrc (java=<version>-librca).
FROM bellsoft/liberica-openjdk-debian:25.0.3

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates curl unzip coreutils libdigest-sha-perl gpg vim nano sudo psmisc \
        locales rsync git \
    && rm -rf /var/lib/apt/lists/*

# Source releases omit the wrapper JAR, so their bootstrap build needs Gradle on PATH.
# grails-core bakes in its own gradlew instead; here the wrapper is regenerated from
# gradle-bootstrap, which needs a real Gradle to run.
COPY .sdkmanrc /tmp/project.sdkmanrc
RUN set -eu; \
    GRADLE_VERSION="$(sed -n 's/^gradle=//p' /tmp/project.sdkmanrc)"; \
    test -n "$GRADLE_VERSION"; \
    GRADLE_URL="https://services.gradle.org/distributions/gradle-${GRADLE_VERSION}-bin.zip"; \
    curl --fail --location --retry 3 "$GRADLE_URL" --output /tmp/gradle.zip; \
    GRADLE_SHA256="$(curl --fail --location --retry 3 "${GRADLE_URL}.sha256")"; \
    printf '%s  /tmp/gradle.zip\n' "$GRADLE_SHA256" | sha256sum --check -; \
    unzip -q /tmp/gradle.zip -d /opt; \
    ln -s "/opt/gradle-${GRADLE_VERSION}/bin/gradle" /usr/local/bin/gradle; \
    rm /tmp/gradle.zip /tmp/project.sdkmanrc

RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen \
    && dpkg-reconfigure --frontend=noninteractive locales \
    && update-locale LANG=en_US.UTF-8

# Login shells rebuild PATH from /etc/profile, which drops both the JDK and the baked-in
# scripts. This restores them and prints what the container offers on an interactive shell.
COPY etc/bin/container-profile.sh /etc/profile.d/grails-ij.sh

RUN useradd --system --create-home --home-dir /home/groovy groovy
RUN usermod -s /bin/bash -g root -G sudo groovy
RUN echo '%sudo ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
RUN echo '. /etc/profile.d/grails-ij.sh' >> /home/groovy/.bashrc
USER groovy

WORKDIR /home/groovy
RUN mkdir -p /home/groovy/scripts/etc/bin /home/groovy/grails-verify /home/groovy/project
ADD --chown=groovy etc/bin /home/groovy/scripts/etc/bin

# Defensive line-ending normalization. The repository's .gitattributes pins shell scripts
# and the Dockerfile to LF on every platform, but committers with a pre-existing local
# checkout under core.autocrlf=true may still feed CRLF into the build context, and Linux
# refuses scripts with bash\r shebangs. Strip CRs from any text file that must be LF.
RUN find /home/groovy/scripts -type f \( -name '*.sh' -o -name '*.properties' \) \
        -exec sed -i 's/\r$//' {} \;

ENV PATH="/home/groovy/scripts:/home/groovy/scripts/etc/bin:$PATH"
ENV CI=true
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8
ENV LC_CTYPE=en_US.UTF-8

CMD ["/bin/bash", "-ec", "while :; do echo '.'; sleep 1000 ; done"]

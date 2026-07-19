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

# A container that resembles the GitHub Actions Linux build environment, used to test
# reproducible builds independently of the host OS. Mount the project directory into the
# container so builds run in the container but artifacts can be compared on the host.
#
# Build & run from the project root:
#   docker build -t grails-ij:testing -f etc/bin/Dockerfile .
#   docker run -it --rm -v "$(pwd):/home/builder/project" grails-ij:testing bash
# Then, inside the container:
#   cd project && etc/bin/test-reproducible-build.sh
#
# Keep the JDK version synced with .sdkmanrc (java=<version>-librca).
FROM bellsoft/liberica-openjdk-debian:25.0.3

USER root
RUN apt-get update \
    && apt-get install -y --no-install-recommends \
        ca-certificates curl unzip coreutils git gpg sudo psmisc locales \
    && rm -rf /var/lib/apt/lists/*

RUN sed -i -e 's/# en_US.UTF-8 UTF-8/en_US.UTF-8 UTF-8/' /etc/locale.gen \
    && dpkg-reconfigure --frontend=noninteractive locales \
    && update-locale LANG=en_US.UTF-8

RUN useradd --create-home --home-dir /home/builder builder \
    && echo 'builder ALL=(ALL) NOPASSWD:ALL' >> /etc/sudoers
USER builder
WORKDIR /home/builder/project

ENV CI=true
ENV LANG=en_US.UTF-8
ENV LC_ALL=en_US.UTF-8

CMD ["/bin/bash"]

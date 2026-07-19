/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
plugins {
    `kotlin-dsl`
}

// The convention plugins here are consumed by the root build and every module. The
// vulnerability-scan plugin applies the Sonatype OSS Index plugin and references its
// extension type, so that plugin must be on the build-logic compile classpath.
dependencies {
    // Dependabot (gradle ecosystem, /build-logic) keeps this version current.
    implementation("org.sonatype.gradle.plugins:scan-gradle-plugin:3.1.6")
}

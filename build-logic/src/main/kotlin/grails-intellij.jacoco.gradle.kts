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
import org.gradle.testing.jacoco.tasks.JacocoReport

// Per-project JaCoCo conventions. Ported from grails-core's GrailsJacocoPlugin (per-module
// portion only). The cross-project aggregate is produced by the root build via Gradle's
// built-in `jacoco-report-aggregation` plugin, which is safe under the configuration cache
// (this repo has org.gradle.configuration-cache=true, unlike grails-core).

plugins {
    jacoco
}

jacoco {
    // keep in sync with `jacocoVersion` in gradle.properties
    toolVersion = (project.findProperty("jacocoVersion") ?: "0.8.14").toString()
}

tasks.withType<Test>().configureEach {
    finalizedBy(tasks.withType<JacocoReport>())
}

tasks.withType<JacocoReport>().configureEach {
    dependsOn(tasks.withType<Test>())
    reports {
        xml.required.set(true)
        html.required.set(true)
        csv.required.set(false)
    }
}

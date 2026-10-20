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

package org.apache.grails.intellij.build.sbom

import groovy.json.JsonSlurper
import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatThrownBy

/**
 * The rewrite the task applies to what CycloneDX wrote. The task itself runs in {@link SbomPluginTest}.
 */
class ReproducibleSbomTaskTest {

    private static final Logger LOGGER = Logging.getLogger(ReproducibleSbomTaskTest)
    private static final String TIMESTAMP = '1970-01-01T00:00:00Z'

    @Test
    void 'the rewritten SBOM is dated as told, records one license per dependency and sorts what resolution ordered'() {
        Map rendered = render(bom(), ':plugin')

        assertThat(rendered.metadata.timestamp).isEqualTo(TIMESTAMP)
        Map library = rendered.components.find { it.name == 'library' }
        assertThat(library.licenses).isEqualTo([[license: [id: 'Apache-2.0', url: 'https://www.apache.org/licenses/LICENSE-2.0.txt']]])
        // an external dependency keeps the hashes of its jar
        assertThat(library.hashes).isNotEmpty()
        assertThat(rendered.dependencies.find { it.ref == 'pkg:maven/org.example/app@1.0' }.dependsOn)
                .isEqualTo(['pkg:maven/org.example/library@1.0?type=jar', 'pkg:maven/org.example/module@1.0?project_path=%3Amodule'])
        assertThat(rendered.serialNumber as String).startsWith('urn:uuid:')
    }

    @Test
    void 'the hashes of the build\'s own projects are dropped, since their jars may not exist yet'() {
        Map rendered = render(bom(), ':plugin')

        Map module = rendered.components.find { it.name == 'module' }
        assertThat(module).doesNotContainKey('hashes')
        assertThat(module).doesNotContainKey('licenses')
    }

    @Test
    void 'the same content gives the same file, and another project the same content gives another serial number'() {
        String first = ReproducibleSbomTask.render(bom(), ':plugin', 'app', TIMESTAMP, LOGGER)
        String again = ReproducibleSbomTask.render(bom(), ':plugin', 'app', TIMESTAMP, LOGGER)
        String other = ReproducibleSbomTask.render(bom(), ':other', 'app', TIMESTAMP, LOGGER)

        assertThat(again).isEqualTo(first)
        assertThat(first).endsWith('}\n')
        assertThat(((Map) new JsonSlurper().parseText(other)).serialNumber)
                .isNotEqualTo(((Map) new JsonSlurper().parseText(first)).serialNumber)
    }

    @Test
    void 'a dependency under a license the policy does not permit fails the rewrite'() {
        Map bom = bom()
        ((List) bom.components).find { it.name == 'library' }.licenses = [[license: [id: 'GPL-2.0-only']]]

        assertThatThrownBy { ReproducibleSbomTask.render(bom, ':plugin', 'app', TIMESTAMP, LOGGER) }
                .isInstanceOf(GradleException)
                .hasMessage('Unpermitted license GPL-2.0-only for the dependency pkg:maven/org.example/library@1.0?type=jar of app')
    }

    private static Map render(Map bom, String projectPath) {
        (Map) new JsonSlurper().parseText(ReproducibleSbomTask.render(bom, projectPath, 'app', TIMESTAMP, LOGGER))
    }

    /** What the CycloneDX plugin writes for an application with one external and one project dependency. */
    private static Map bom() {
        [
                bomFormat   : 'CycloneDX',
                specVersion : '1.6',
                serialNumber: 'urn:uuid:' + UUID.randomUUID(),
                version     : 1,
                metadata    : [
                        timestamp: '2026-10-20T17:00:00Z',
                        component: [type: 'application', 'bom-ref': 'pkg:maven/org.example/app@1.0', name: 'app', version: '1.0'],
                ],
                components  : [
                        [type     : 'library', 'bom-ref': 'pkg:maven/org.example/library@1.0?type=jar', group: 'org.example',
                         name     : 'library', version: '1.0',
                         hashes   : [[alg: 'SHA-256', content: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855']],
                         licenses : [[license: [id: 'LGPL-2.1-only']],
                                     [license: [id: 'Apache-2.0', url: 'https://www.apache.org/licenses/LICENSE-2.0.txt']]],
                         purl     : 'pkg:maven/org.example/library@1.0?type=jar'],
                        [type  : 'library', 'bom-ref': 'pkg:maven/org.example/module@1.0?project_path=%3Amodule', group: 'org.example',
                         name  : 'module', version: '1.0',
                         hashes: [[alg: 'SHA-256', content: 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855']]],
                ],
                dependencies: [
                        [ref      : 'pkg:maven/org.example/app@1.0',
                         dependsOn: ['pkg:maven/org.example/module@1.0?project_path=%3Amodule', 'pkg:maven/org.example/library@1.0?type=jar']],
                        [ref: 'pkg:maven/org.example/library@1.0?type=jar', dependsOn: []],
                        [ref: 'pkg:maven/org.example/module@1.0?project_path=%3Amodule', dependsOn: []],
                ],
        ]
    }
}

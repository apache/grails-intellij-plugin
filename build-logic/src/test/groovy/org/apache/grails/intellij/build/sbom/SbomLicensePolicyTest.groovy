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

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.junit.Test

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.assertThatThrownBy

class SbomLicensePolicyTest {

    private static final Logger LOGGER = Logging.getLogger(SbomLicensePolicyTest)
    private static final String BOM_REF = 'pkg:maven/org.example/library@1.0?type=jar'

    @Test
    void 'of several licenses the most preferred one is recorded'() {
        List choices = [[license: [id: 'LGPL-2.1-only']], [license: [id: 'MIT']], [license: [id: 'EPL-2.0']]]

        assertThat(SbomLicensePolicy.pickLicense(LOGGER, 'app', BOM_REF, choices)).isEqualTo([license: [id: 'EPL-2.0']])
    }

    @Test
    void 'a single permitted license is kept as it is, with its url'() {
        Map choice = [license: [id: 'Apache-2.0', url: 'https://www.apache.org/licenses/LICENSE-2.0.txt']]

        assertThat(SbomLicensePolicy.pickLicense(LOGGER, 'app', BOM_REF, [choice])).isSameAs(choice)
    }

    @Test
    void 'a license the policy does not permit fails, naming the dependency and the component'() {
        assertThatThrownBy { SbomLicensePolicy.pickLicense(LOGGER, 'app', BOM_REF, [[license: [id: 'GPL-2.0-only']]]) }
                .isInstanceOf(GradleException)
                .hasMessage("Unpermitted license GPL-2.0-only for the dependency ${BOM_REF} of app")
    }

    @Test
    void 'a dependency without a license, or with one that has no id, fails'() {
        assertThatThrownBy { SbomLicensePolicy.pickLicense(LOGGER, 'app', BOM_REF, []) }
                .isInstanceOf(GradleException)
                .hasMessage("No license was found for the dependency ${BOM_REF} of app")
        assertThatThrownBy { SbomLicensePolicy.pickLicense(LOGGER, 'app', BOM_REF, [[license: [name: 'The BSD License']]]) }
                .isInstanceOf(GradleException)
                .hasMessageStartingWith("Could not determine the license id of the dependency ${BOM_REF} of app")
        assertThatThrownBy { SbomLicensePolicy.pickLicense(LOGGER, 'app', null, [[license: [id: 'MIT']]]) }
                .isInstanceOf(GradleException)
                .hasMessage('No bom-ref found for a dependency of app, cannot pick its license')
    }

    @Test
    void 'the policy tables name only licenses the preferred list and the license blocks know'() {
        SbomLicensePolicy.LICENSE_MAPPING.each { String bomRef, String id ->
            assertThat(SbomLicensePolicy.LICENSES).describedAs(bomRef).containsKey(id)
        }
        SbomLicensePolicy.LICENSES.each { String id, Map<String, String> license ->
            assertThat(license.id).isEqualTo(id)
            assertThat(SbomLicensePolicy.PREFERRED_LICENSES + ['BSD-2-Clause']).describedAs(id).contains(id)
        }
        // nothing in the plugin's distribution comes from outside this repository yet
        assertThat(SbomLicensePolicy.LICENSE_MAPPING).isEmpty()
        assertThat(SbomLicensePolicy.LICENSE_EXCEPTIONS).isEmpty()
    }
}

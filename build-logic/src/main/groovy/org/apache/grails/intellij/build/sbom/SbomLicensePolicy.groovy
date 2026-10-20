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

import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger

/**
 * Chooses, for each dependency in an SBOM, the one license it is recorded under, as ASF policy
 * (https://www.apache.org/legal/resolved.html) wants it: a dependency offered under several licenses is recorded
 * under the most preferred one, a dependency whose published metadata is wrong is corrected from a mapping, and a
 * dependency under a license the policy does not permit fails the build unless it is explicitly exempted.
 *
 * <p>Ported from grails-core's {@code SbomPlugin}. The tables are the policy of this build: add to
 * {@link #LICENSE_MAPPING} when CycloneDX reads a dependency's license wrongly, and to {@link #LICENSE_EXCEPTIONS}
 * only for a dependency that is not distributed, or whose use ASF legal has approved.</p>
 */
@CompileStatic
final class SbomLicensePolicy {

    /** Ordered so that the first value is the most preferred, from https://www.apache.org/legal/resolved.html */
    static final List<String> PREFERRED_LICENSES = [
            'Apache-2.0', 'EPL-1.0', 'BSD-3-Clause', 'EPL-2.0', 'MIT', 'MIT-0', '0BSD', 'UPL-1.0',
            'CC0-1.0', 'ICU', 'Xnet', 'NCSA', 'W3C', 'Zlib', 'AFL-3.0', 'MS-PL', 'PSF-2.0', 'APAFML',
            'BSL-1.0', 'WTFPL', 'Unlicense', 'HPND', 'EPICS', 'TCL',
    ].asImmutable()

    /** The license blocks a mapping can record a dependency under; ids are the SPDX ones, https://spdx.org/licenses/ */
    static final Map<String, Map<String, String>> LICENSES = [
            'Apache-2.0'  : [id: 'Apache-2.0', url: 'https://www.apache.org/licenses/LICENSE-2.0'],
            'BSD-2-Clause': [id: 'BSD-2-Clause', url: 'https://opensource.org/license/bsd-2-clause/'],
            'BSD-3-Clause': [id: 'BSD-3-Clause', url: 'https://opensource.org/license/bsd-3-clause/'],
            'CC0-1.0'     : [id: 'CC0-1.0', url: 'https://creativecommons.org/publicdomain/zero/1.0/'],
            'MIT'         : [id: 'MIT', url: 'https://opensource.org/license/mit/'],
            'UPL-1.0'     : [id: 'UPL-1.0', url: 'https://oss.oracle.com/licenses/upl/'],
    ].asImmutable() as Map<String, Map<String, String>>

    /**
     * Dependencies whose published license metadata CycloneDX reads wrongly, by bom-ref (a package URL), and the
     * license they are actually under. Each entry says why, with a link; most are the name-only "The BSD License"
     * problem of https://github.com/CycloneDX/cyclonedx-core-java/issues/205. Empty until the plugin ships a
     * dependency that needs one: everything in its distribution is currently built from this repository.
     */
    static final Map<String, String> LICENSE_MAPPING = [:].asImmutable() as Map<String, String>

    /**
     * Dependencies under a license outside {@link #PREFERRED_LICENSES} that a component may nevertheless list, by
     * component name and then bom-ref, with the license id that is permitted. A Category X license may appear here
     * only for a dependency that is not distributed, or whose use ASF legal has approved in a LEGAL ticket named in
     * the entry. The entries are version-specific on purpose, so a dependency bump revisits them.
     */
    static final Map<String, Map<String, String>> LICENSE_EXCEPTIONS = [:].asImmutable() as Map<String, Map<String, String>>

    private SbomLicensePolicy() {
    }

    /**
     * Picks the license a dependency is recorded under, from the choices CycloneDX found for it.
     *
     * @param logger where the choice is logged
     * @param component the name of the component the SBOM describes, which the exemptions are keyed by
     * @param bomRef the bom-ref of the dependency, a package URL
     * @param licenseChoices the {@code licenses} entries of the dependency in the SBOM
     * @return the license entry to keep
     * @throws GradleException when the dependency has no license, or one the policy does not permit
     */
    @CompileDynamic
    static Object pickLicense(Logger logger, String component, String bomRef, List licenseChoices) {
        if (!bomRef) {
            throw new GradleException("No bom-ref found for a dependency of ${component}, cannot pick its license")
        }

        logger.info('Picking the license of {} from {} choices', bomRef, licenseChoices.size())
        if (LICENSE_MAPPING.containsKey(bomRef)) {
            String licenseId = LICENSE_MAPPING[bomRef]
            logger.info('Recording {} under {} as the mapping says', bomRef, licenseId)
            Map<String, String> license = LICENSES[licenseId]
            if (!license) {
                throw new GradleException("No license block for the id ${licenseId} that ${bomRef} of ${component} is mapped to")
            }
            return [license: license]
        }

        if (!(licenseChoices instanceof List) || licenseChoices.isEmpty()) {
            throw new GradleException("No license was found for the dependency ${bomRef} of ${component}")
        }

        List identified = licenseChoices.findAll { it instanceof Map && it.license instanceof Map && it.license.id }
        String preferred = PREFERRED_LICENSES.find { String candidate -> identified.any { it.license.id == candidate } }
        if (preferred) {
            return identified.find { it.license.id == preferred }
        }

        Object first = licenseChoices[0]
        String firstId = first instanceof Map && first.license instanceof Map ? first.license.id as String : null
        if (firstId == null) {
            throw new GradleException("Could not determine the license id of the dependency ${bomRef} of ${component} from ${first}")
        }
        if (!(firstId in PREFERRED_LICENSES)) {
            Map<String, String> exemptions = LICENSE_EXCEPTIONS[component] ?: [:]
            if (exemptions.get(bomRef) != firstId) {
                throw new GradleException("Unpermitted license ${firstId} for the dependency ${bomRef} of ${component}")
            }
        }
        first
    }
}

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

import java.nio.charset.StandardCharsets

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.transform.CompileDynamic
import groovy.transform.CompileStatic

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Turns the CycloneDX SBOM the {@code cyclonedxDirectBom} task wrote into the one that ships: byte-for-byte
 * reproducible, and with every dependency recorded under the one license {@link SbomLicensePolicy} picks for it.
 *
 * <p>What the CycloneDX plugin writes is not reproducible: its timestamp is the build time, its serial number is a
 * random UUID, and the order of a component's dependencies follows resolution. The timestamp is pinned, the
 * dependency lists are sorted, the hashes of the build's own project components are dropped (they are the hashes of
 * jars that may not exist yet when a parallel build generates the SBOM), and the serial number is derived from the
 * content, so the same dependencies always give the same file. The license pass runs on every build, so a
 * dependency under a license ASF policy does not permit fails the build that would ship it.</p>
 *
 * <p>Ported from the {@code doLast} of grails-core's {@code SbomPlugin}, as a task with declared inputs and
 * outputs so it is cacheable and the jar depends on its output rather than on the generator.</p>
 */
@CompileStatic
@CacheableTask
abstract class ReproducibleSbomTask extends DefaultTask {

    /** The SBOM as the CycloneDX plugin wrote it. */
    @InputFile
    @PathSensitive(PathSensitivity.NONE)
    abstract RegularFileProperty getBom()

    /** The name of the component the SBOM describes; the license exemptions are keyed by it. */
    @Input
    abstract Property<String> getComponentName()

    /** The path of the project, mixed into the serial number so identical SBOMs of two projects still differ. */
    @Input
    abstract Property<String> getProjectPath()

    /** The ISO-8601 instant recorded as the SBOM's timestamp. */
    @Input
    abstract Property<String> getTimestamp()

    @OutputFile
    abstract RegularFileProperty getSbom()

    @TaskAction
    void write() {
        Map bom = (Map) new JsonSlurper().parse(getBom().get().asFile, StandardCharsets.UTF_8.name())
        String content = render(bom, projectPath.get(), componentName.get(), timestamp.get(), logger)
        File output = sbom.get().asFile
        output.parentFile.mkdirs()
        output.setText(content, StandardCharsets.UTF_8.name())
        logger.info('Wrote the reproducible SBOM of {} to {}', componentName.get(), output)
    }

    /**
     * The reproducible SBOM, as pretty-printed JSON (https://cyclonedx.org/docs/1.6/json/).
     *
     * @param bom the SBOM as the CycloneDX plugin wrote it; it is modified in place
     * @param projectPath the path of the project, mixed into the serial number
     * @param component the name of the component the SBOM describes
     * @param timestamp the ISO-8601 instant to record
     * @param logger where the license choices are logged
     */
    @CompileDynamic
    static String render(Map bom, String projectPath, String component, String timestamp, Logger logger) {
        bom.metadata = (bom.metadata instanceof Map ? bom.metadata : [:]) as Map
        bom.metadata.timestamp = timestamp

        List components = bom.components instanceof List ? bom.components : []
        components.each { Object entry ->
            if (!(entry instanceof Map)) {
                return
            }
            Map c = (Map) entry
            // one license per dependency, the one ASF policy prefers; a dependency without any is left as it is
            if (c.licenses instanceof List && !((List) c.licenses).empty) {
                Object chosen = SbomLicensePolicy.pickLicense(logger, component, c['bom-ref'] as String, (List) c.licenses)
                if (chosen != null) {
                    c.licenses = [chosen]
                }
            }
            // the build's own projects are listed with the hashes of their jars, which a parallel build may not
            // have produced yet when the SBOM is generated; nothing reproducible can be recorded for them
            if (c.hashes instanceof List && (c['bom-ref'] as String)?.contains('?project_path=')) {
                c.remove('hashes')
            }
        }

        List dependencies = bom.dependencies instanceof List ? bom.dependencies : []
        dependencies.each { Object entry ->
            if (entry instanceof Map && ((Map) entry).dependsOn instanceof List) {
                ((Map) entry).dependsOn = (((Map) entry).dependsOn as List).sort(false)
            }
        }

        // The serial number is derived from the content, with the project path mixed in so two projects whose
        // SBOMs happen to be identical (empty ones, say) still get the distinct serial numbers the specification
        // requires, see https://cyclonedx.org/docs/1.6/json/#serialNumber
        bom.serialNumber = ''
        String withoutSerial = JsonOutput.prettyPrint(JsonOutput.toJson(bom))
        bom.serialNumber = "urn:uuid:${UUID.nameUUIDFromBytes("${projectPath}\n${withoutSerial}".getBytes(StandardCharsets.UTF_8))}".toString()
        JsonOutput.prettyPrint(JsonOutput.toJson(bom)) + '\n'
    }
}

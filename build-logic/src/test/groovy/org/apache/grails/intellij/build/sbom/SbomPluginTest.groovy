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

import java.util.jar.JarFile
import java.util.jar.JarOutputStream
import java.util.jar.Manifest

import groovy.json.JsonSlurper
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.assertj.core.api.Assertions.assertThat

/**
 * Runs the {@code sbom} convention plugin through Gradle TestKit on a library with a dependency from a local Maven
 * repository, with the configuration cache on and its problems fatal, as the real build runs.
 */
class SbomPluginTest {

    private static final String SBOM_PLUGIN = 'org.apache.grails.intellij.build.sbom'

    @Rule
    public final TemporaryFolder tmp = new TemporaryFolder()

    private File projectDir
    private File repositoryDir

    @Before
    void setUp() {
        projectDir = tmp.newFolder('project')
        repositoryDir = tmp.newFolder('repository')
        publish('org.example', 'library', '1.0', 'Apache License, Version 2.0', 'https://www.apache.org/licenses/LICENSE-2.0.txt')
        new File(projectDir, 'settings.gradle').text = "rootProject.name = 'app'"
        new File(projectDir, 'gradle.properties').text = '''\
            org.gradle.configuration-cache=true
            org.gradle.configuration-cache.problems=fail
            version=1.0.0
            '''.stripIndent()
    }

    @Test
    void 'the SBOM ships in the jar, reproducibly, with the configuration cache stored and then reused'() {
        writeProject(['org.example:library:1.0'])

        BuildResult stored = runner('clean', 'jar').build()
        File sbom = new File(projectDir, 'build/sbom/app-1.0.0-sbom.json')
        String first = sbom.text
        byte[] firstJar = new File(projectDir, 'build/libs/app-1.0.0.jar').bytes
        BuildResult reused = runner('clean', 'jar').build()

        assertThat(stored.output).contains('Configuration cache entry stored')
        assertThat(reused.output).contains('Configuration cache entry reused')
        assertThat(stored.task(':sbom').outcome).isEqualTo(TaskOutcome.SUCCESS)

        // reproducible: dated at the epoch, with a serial number derived from the content, so a rebuild is identical
        assertThat(first)
                .contains('"timestamp": "1970-01-01T00:00:00Z"')
                .contains('"serialNumber": "urn:uuid:')
        assertThat(sbom.text).isEqualTo(first)
        assertThat(new File(projectDir, 'build/libs/app-1.0.0.jar').bytes).isEqualTo(firstJar)

        // what it describes: the application and its one dependency, under the one license ASF policy prefers
        Map bom = (Map) new JsonSlurper().parseText(first)
        assertThat(bom.metadata.component.name).isEqualTo('app')
        assertThat(bom.metadata.component.type).isEqualTo('application')
        assertThat(bom.metadata.component.version).isEqualTo('1.0.0')
        Map library = bom.components.find { it.name == 'library' }
        assertThat(library.licenses*.license*.id).containsExactly('Apache-2.0')
        assertThat(bom.metadata.manufacturer.name).isEqualTo('Apache Software Foundation')
        assertThat(bom.metadata.licenses*.license*.id).containsExactly('Apache-2.0')
        assertThat(bom.metadata.component.externalReferences*.type).contains('website', 'vcs', 'mailing-list')
        assertThat(bom.metadata.component.externalReferences.find { it.type == 'mailing-list' }.url).isEqualTo('users@grails.apache.org')

        // packaged into the jar, named by the manifest
        new JarFile(new File(projectDir, 'build/libs/app-1.0.0.jar')).withCloseable { JarFile jar ->
            assertThat(jar.getInputStream(jar.getJarEntry('META-INF/sbom.json')).text).isEqualTo(first)
            assertThat(jar.manifest.mainAttributes.getValue('Sbom-Location')).isEqualTo('META-INF/sbom.json')
            assertThat(jar.manifest.mainAttributes.getValue('Sbom-Format')).isEqualTo('CycloneDX')
        }
    }

    @Test
    void 'a snapshot is referred to the development list, and SOURCE_DATE_EPOCH dates the SBOM'() {
        writeProject(['org.example:library:1.0'])
        new File(projectDir, 'gradle.properties') << 'version=1.0.1-SNAPSHOT\n'

        runner('sbom').withEnvironment(System.getenv() + [SOURCE_DATE_EPOCH: '1700000000']).build()

        Map bom = (Map) new JsonSlurper().parse(new File(projectDir, 'build/sbom/app-1.0.1-SNAPSHOT-sbom.json'))
        assertThat(bom.metadata.timestamp).isEqualTo('2023-11-14T22:13:20Z')
        assertThat(bom.metadata.component.externalReferences.find { it.type == 'mailing-list' }.url).isEqualTo('dev@grails.apache.org')
    }

    @Test
    void 'a dependency whose license the policy cannot place fails the build before anything is packaged'() {
        // a license CycloneDX cannot map to an SPDX id is recorded by name only, and a name is not a verdict
        publish('org.example', 'copyleft', '1.0', 'GNU General Public License, version 2', 'https://www.gnu.org/licenses/gpl-2.0.html')
        writeProject(['org.example:library:1.0', 'org.example:copyleft:1.0'])

        BuildResult result = runner('jar').buildAndFail()

        assertThat(result.task(':sbom').outcome).isEqualTo(TaskOutcome.FAILED)
        assertThat(result.task(':jar')).isNull()
        assertThat(result.output).contains('Could not determine the license id of the dependency ' +
                'pkg:maven/org.example/copyleft@1.0?type=jar of app from [license:[name:GNU General Public License, version 2')
    }

    private GradleRunner runner(String... arguments) {
        GradleRunner.create()
                .withProjectDir(projectDir)
                .withArguments(arguments.toList() + ['--stacktrace'])
                .withPluginClasspath()
    }

    private void writeProject(List<String> dependencies) {
        new File(projectDir, 'build.gradle').text = """
            plugins {
                id 'java-library'
                id '$SBOM_PLUGIN'
            }
            group = 'org.example'
            repositories {
                maven { url = uri('${repositoryDir.toURI()}') }
            }
            dependencies {
                ${dependencies.collect { "implementation '${it}'" }.join('\n                ')}
            }
        """
    }

    private void publish(String group, String artifact, String version, String licenseName, String licenseUrl) {
        File dir = new File(repositoryDir, "${group.replace('.', '/')}/${artifact}/${version}")
        dir.mkdirs()
        new File(dir, "${artifact}-${version}.pom").text = """<?xml version="1.0" encoding="UTF-8"?>
            <project xmlns="http://maven.apache.org/POM/4.0.0">
                <modelVersion>4.0.0</modelVersion>
                <groupId>${group}</groupId>
                <artifactId>${artifact}</artifactId>
                <version>${version}</version>
                <licenses>
                    <license>
                        <name>${licenseName}</name>
                        <url>${licenseUrl}</url>
                    </license>
                </licenses>
            </project>
        """.stripIndent().trim()
        // a valid, empty jar: CycloneDX opens each artifact to look for a POM inside it
        new JarOutputStream(new File(dir, "${artifact}-${version}.jar").newOutputStream(), new Manifest()).close()
    }
}

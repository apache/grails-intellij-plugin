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

import groovy.transform.CompileStatic

import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property

/**
 * Configures the SBOM of a project, as the {@code sbom} extension registered by the
 * {@code org.apache.grails.intellij.build.sbom} convention plugin.
 */
@CompileStatic
abstract class SbomExtension {

    /** The name of the component the SBOM describes, the project name unless set. */
    abstract Property<String> getComponentName()

    /** The configurations whose resolved dependencies the SBOM lists, {@code runtimeClasspath} unless set. */
    abstract ListProperty<String> getConfigurations()
}

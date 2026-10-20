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

package org.apache.grails.intellij.build.scan

import groovy.transform.CompileStatic

/**
 * The vulnerabilities no project of this build reports.
 *
 * <p>Exclusions are limited to vulnerabilities with no available upstream fix, a breaking major-version upgrade, or
 * a maintainer-approved false positive backed by an upstream advisory. Everything with a published compatible fix
 * must be bumped instead. Each entry names the vulnerability, the module, the reason and the removal condition, and
 * is revisited whenever the dependency moves. The list is empty until a real, unfixable finding is encountered.</p>
 *
 * <p>A single project excludes a vulnerability of its own with {@code vulnerabilityScan { exclude(...) }} in its
 * build file, which adds to this list.</p>
 */
@CompileStatic
final class BuiltInExclusions {

    static final List<VulnerabilityExclusion> EXCLUSIONS = [].asImmutable() as List<VulnerabilityExclusion>

    private BuiltInExclusions() {
    }
}

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
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.apache.grails.intellij.build

import org.gradle.api.provider.Property

/**
 * Backs the {@code legacyBytecode} block of the java-legacy convention plugin.
 *
 * <p>Three projects ship code that runs inside an end user's JVM rather than the IDE, so they
 * must emit older bytecode than the IntelliJ platform's Java level: grails-rt and
 * grails-compiler-patch target 8, jps-plugin targets 11 (it runs in the JPS build process).
 * The IntelliJ Platform Gradle Plugin force-sets {@code options.release} to the platform's Java
 * version, so these projects have to set it back afterwards.
 */
abstract class LegacyBytecodeExtension {

    /** Java release level to compile against. Defaults to 8. */
    abstract Property<Integer> getRelease()
}

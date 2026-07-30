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

package org.apache.grails.intellij.plugin.spring;

import com.intellij.openapi.application.AccessToken;
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

final class SpringModelAccess {

  private SpringModelAccess() {
  }

  /**
   * Runs a Spring model lookup with the platform's "the expensive method should not be called during the references
   * contributing" assertion suppressed.
   * <p>
   * Grails resolves injected beans and {@code applicationContext} members through the Spring model, so Groovy
   * reference resolution depends on it. Reference contributors resolve Groovy code themselves — IntelliLang, for
   * instance, resolves the enclosing call to find out whether a literal carries an injected language — which drags
   * the Spring model into a phase where {@code SpringManager} and {@code SpringModelUtils} assert against it.
   * <p>
   * Skipping the lookup instead is not an option: the results feed {@code CachedValue}s that outlive the pass, so a
   * degraded type computed here would be served to every later caller.
   *
   * @see <a href="https://github.com/apache/grails-intellij-plugin/issues/16">issue 16</a>
   */
  static <T> T compute(@NotNull Supplier<T> lookup) {
    try (AccessToken ignored = ReferenceProvidersRegistry.suppressAssertNotContributingReferences()) {
      return lookup.get();
    }
  }
}

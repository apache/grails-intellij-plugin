/*
 * Copyright 2000-2026 JetBrains s.r.o. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.plugins.grails.projectView.nodes

import com.intellij.util.lazyUnsafe

/**
 * Represents a fully qualified name with compacted part.
 *
 * Consider `com.foo.bar.baz.bax` fqn in the view under `com.foo` node.
 * ```
 * - com
 *   - foo
 *     - bar.baz.bax // <- this node
 *   + bad
 *   + goo
 *   ...
 * ```
 * @param baseFqn possibly empty list of parts of the base fqn, e.g. `["com", "foo"]`
 * @param relativeParts non-empty list of parts of the fqn relative to the [baseFqn], e.g. `["bar", "baz", "bax"]`
 *
 */
data class CompactedFqn(val baseFqn: List<String>, val relativeParts: List<String>) {

  init {
    require(relativeParts.isNotEmpty())
  }

  val hasCompactedFqns: Boolean = relativeParts.size > 1

  /**
   * All parts of this fqn, e.g.: `["com", "foo", "bar", "bar", "bax"]`.
   */
  val allParts: List<String> = baseFqn + relativeParts

  /**
   * All expanded fqns, e.g.: `["com.foo.bar", "com.foo.bar.baz", "com.foo.bar.baz.bax"]`.
   * The list starts from the parent fqn.
   */
  val expandedFqns: List<String> by lazyUnsafe {
    val result = ArrayList<String>(relativeParts.size)
    val current = baseFqn.toMutableList()
    for (relativePart in relativeParts) {
      current.add(relativePart)
      result.add(current.fqnString())
    }
    result
  }

  override fun toString(): String = allParts.fqnString()
}

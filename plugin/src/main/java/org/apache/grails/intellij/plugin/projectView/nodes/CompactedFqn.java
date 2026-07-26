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

package org.apache.grails.intellij.plugin.projectView.nodes;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Represents a fully qualified name with compacted part.
 *
 * <p>Consider {@code com.foo.bar.baz.bax} fqn in the view under {@code com.foo} node.
 * <pre>
 * - com
 *   - foo
 *     - bar.baz.bax // &lt;- this node
 *   + bad
 *   + goo
 *   ...
 * </pre>
 */
public final class CompactedFqn {

  private final List<String> baseFqn;
  private final List<String> relativeParts;
  private final boolean hasCompactedFqns;
  private final List<String> allParts;

  // Deferred because it is only needed when the node is actually rendered or validated.
  private volatile List<String> expandedFqns;

  /**
   * @param baseFqn       possibly empty list of parts of the base fqn, e.g. {@code ["com", "foo"]}
   * @param relativeParts non-empty list of parts of the fqn relative to the baseFqn, e.g. {@code ["bar", "baz", "bax"]}
   */
  public CompactedFqn(@NotNull List<String> baseFqn, @NotNull List<String> relativeParts) {
    if (relativeParts.isEmpty()) {
      throw new IllegalArgumentException("relativeParts must not be empty");
    }
    this.baseFqn = List.copyOf(baseFqn);
    this.relativeParts = List.copyOf(relativeParts);
    this.hasCompactedFqns = this.relativeParts.size() > 1;

    List<String> all = new ArrayList<>(this.baseFqn);
    all.addAll(this.relativeParts);
    this.allParts = List.copyOf(all);
  }

  public @NotNull List<String> getBaseFqn() {
    return baseFqn;
  }

  public @NotNull List<String> getRelativeParts() {
    return relativeParts;
  }

  public boolean getHasCompactedFqns() {
    return hasCompactedFqns;
  }

  /** All parts of this fqn, e.g.: {@code ["com", "foo", "bar", "bar", "bax"]}. */
  public @NotNull List<String> getAllParts() {
    return allParts;
  }

  /**
   * All expanded fqns, e.g.: {@code ["com.foo.bar", "com.foo.bar.baz", "com.foo.bar.baz.bax"]}.
   * The list starts from the parent fqn.
   */
  public @NotNull List<String> getExpandedFqns() {
    List<String> result = expandedFqns;
    if (result == null) {
      List<String> computed = new ArrayList<>(relativeParts.size());
      List<String> current = new ArrayList<>(baseFqn);
      for (String relativePart : relativeParts) {
        current.add(relativePart);
        computed.add(GrailsNodes.fqnString(current));
      }
      result = List.copyOf(computed);
      expandedFqns = result;
    }
    return result;
  }

  // Kotlin's data class equality covered only the primary constructor properties; the
  // derived members are a function of these two, so this stays equivalent.
  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (!(o instanceof CompactedFqn other)) return false;
    return baseFqn.equals(other.baseFqn) && relativeParts.equals(other.relativeParts);
  }

  @Override
  public int hashCode() {
    return Objects.hash(baseFqn, relativeParts);
  }

  @Override
  public String toString() {
    return GrailsNodes.fqnString(allParts);
  }
}

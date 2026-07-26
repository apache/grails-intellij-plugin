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
package org.apache.grails.intellij.plugin.runner;

/**
 * Persisted state of {@link GrailsConsole}. The accessor names are part of the serialized form —
 * {@code autoCloseEnabled} is written as an {@code <option>} element — so do not rename them.
 */
public final class GrailsConsoleState {

  private boolean autoCloseEnabled = true;

  public GrailsConsoleState() {
  }

  public GrailsConsoleState(boolean autoCloseEnabled) {
    this.autoCloseEnabled = autoCloseEnabled;
  }

  public boolean getAutoCloseEnabled() {
    return autoCloseEnabled;
  }

  public void setAutoCloseEnabled(boolean autoCloseEnabled) {
    this.autoCloseEnabled = autoCloseEnabled;
  }

  @Override
  public boolean equals(Object o) {
    return this == o || o instanceof GrailsConsoleState other && autoCloseEnabled == other.autoCloseEnabled;
  }

  @Override
  public int hashCode() {
    return Boolean.hashCode(autoCloseEnabled);
  }

  @Override
  public String toString() {
    return "GrailsConsoleState(autoCloseEnabled=" + autoCloseEnabled + ")";
  }
}

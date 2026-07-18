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

package org.jetbrains.plugins.grails;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.extensions.GroovyScriptTypeDetector;
import org.jetbrains.plugins.groovy.gant.GantScriptType;
import org.jetbrains.plugins.groovy.lang.psi.GroovyFile;

final class GantFileDetector extends GroovyScriptTypeDetector {
  private GantFileDetector() {
    super(GantScriptType.INSTANCE);
  }

  @Override
  public boolean isSpecificScriptFile(@NotNull GroovyFile script) {
    VirtualFile file = script.getVirtualFile();
    if (file == null) return false;

    VirtualFile parent = file.getParent();
    if (parent == null || !parent.getName().equals("scripts")) return false;

    VirtualFile root = parent.getParent();
    if (root == null) return false;

    if (root.findChild("grails-app") == null) return false;

    return true;
  }
}

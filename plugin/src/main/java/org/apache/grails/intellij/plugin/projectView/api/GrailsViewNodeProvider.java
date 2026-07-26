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

package org.apache.grails.intellij.plugin.projectView.api;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.extensions.ExtensionPointName;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;

import java.util.Collection;

public interface GrailsViewNodeProvider {

  // Was a top-level val in ep.kt; as a constant on the interface it stays reachable as
  // GrailsViewNodeProvider.EP_NAME, which matches how platform extension points are declared.
  ExtensionPointName<GrailsViewNodeProvider> EP_NAME =
    ExtensionPointName.create("org.intellij.grails.viewNodeProvider");

  @NotNull Collection<AbstractTreeNode<?>> createNodes(@NotNull GrailsApplication application, @NotNull ViewSettings settings);
}

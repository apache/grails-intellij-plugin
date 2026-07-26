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

package org.jetbrains.plugins.grails.util;

import com.intellij.openapi.util.Condition;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.config.GrailsStructure;

public class GrailsPluginCondition implements Condition<PsiElement> {

  private final String myPluginName;

  public GrailsPluginCondition(@NotNull String pluginName) {
    myPluginName = pluginName;
  }

  @Override
  public boolean value(PsiElement element) {
    GrailsStructure structure = GrailsStructure.getInstance(element);
    if (structure == null) return false;

    return structure.isPluginInstalled(myPluginName);
  }
}

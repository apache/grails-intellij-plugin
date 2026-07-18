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

package org.jetbrains.plugins.grails.projectView;

import com.intellij.ide.IconProvider;
import com.intellij.openapi.util.Iconable;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.ui.IconManager;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler;
import org.jetbrains.plugins.grails.artefact.api.IconOwner;
import org.jetbrains.plugins.grails.artefact.impl.UtilKt;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

import javax.swing.Icon;

final class GrailsIconProvider extends IconProvider {
  @Override
  public @Nullable Icon getIcon(@NotNull PsiElement element, @Iconable.IconFlags int flags) {
    if (element instanceof GrClassDefinition) {
      GrailsArtefactHandler handler = UtilKt.getArtefactHandler((PsiClass)element);
      if (handler instanceof IconOwner) {
        return IconManager.getInstance().createLayeredIcon(element, ((IconOwner)handler).getIcon(), flags);
      }
    }
    return null;
  }
}

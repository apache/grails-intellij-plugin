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
package org.jetbrains.plugins.grails.artefact.impl.controllers;

import com.intellij.openapi.util.Key;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMember;
import com.intellij.psi.ResolveState;
import com.intellij.psi.SyntheticElement;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.grails.util.GrailsUtils;
import org.jetbrains.plugins.grails.util.version.Version;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ControllerActions {

  private ControllerActions() {
  }

  /** Action name to the member declaring it, in declaration order. */
  public static @NotNull Map<String, PsiMember> getActions(@NotNull GrTypeDefinition controller,
                                                           @NotNull GrailsApplication grailsApplication) {
    ActionProcessor processor = new ActionProcessor(grailsApplication.getGrailsVersion().isAtLeast(Version.GRAILS_2_0));
    controller.processDeclarations(processor, ResolveState.initial(), null, controller);
    return processor.getActions();
  }

  private static final class ActionProcessor implements PsiScopeProcessor, ElementClassHint {

    private final boolean myAtLeast14;
    private final Map<String, PsiMember> myActions = new LinkedHashMap<>();

    private ActionProcessor(boolean atLeast14) {
      myAtLeast14 = atLeast14;
    }

    private @NotNull Map<String, PsiMember> getActions() {
      return myActions;
    }

    @Override
    public boolean shouldProcess(@NotNull DeclarationKind kind) {
      return kind == DeclarationKind.FIELD || kind == DeclarationKind.METHOD;
    }

    @Override
    public boolean execute(@NotNull PsiElement element, @NotNull ResolveState state) {
      if (element instanceof SyntheticElement) return true;
      String name = GrailsUtils.getActionName0(element, false, myAtLeast14);
      // First declaration wins, so a subclass override does not displace the base action.
      if (name != null && !myActions.containsKey(name)) {
        myActions.put(name, (PsiMember)element);
      }
      return true;
    }

    @Override
    public <T> @Nullable T getHint(@NotNull Key<T> hintKey) {
      if (hintKey == ElementClassHint.KEY) {
        //noinspection unchecked
        return (T)this;
      }
      return null;
    }
  }
}

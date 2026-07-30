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

package org.apache.grails.intellij.plugin.spring;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.ElementClassHint;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.spring.SpringManager;
import com.intellij.spring.contexts.model.SpringModel;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.utils.SpringModelSearchers;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.config.GrailsStructure;
import org.jetbrains.plugins.groovy.lang.psi.impl.statements.expressions.TypesUtil;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;

import java.util.ArrayList;
import java.util.List;

final class GrailsWebApplicationContextMembersContributor extends NonCodeMembersContributor {
  @Override
  public String getParentClassName() {
    return "org.springframework.context.ApplicationContext";
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @Nullable PsiClass aClass,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!ResolveUtil.shouldProcessProperties(processor.getHint(ElementClassHint.KEY))) return;

    GrailsStructure structure = GrailsStructure.getInstance(place);
    if (structure == null) return;

    Module module = structure.getModule();
    PsiManager manager = structure.getManager();
    String nameHint = ResolveUtil.getNameHint(processor);

    // The beans are collected up front so that the whole Spring model lookup stays inside the single
    // SpringModelAccess scope, and the processor runs outside it.
    for (GrLightVariable bean : SpringModelAccess.compute(() -> collectBeans(module, manager, nameHint))) {
      if (!processor.execute(bean, state)) return;
    }
  }

  private static List<GrLightVariable> collectBeans(@NotNull Module module, @NotNull PsiManager manager, @Nullable String nameHint) {
    final SpringModel model = SpringManager.getInstance(module.getProject()).getCombinedModel(module);

    if (nameHint != null) {
      SpringBeanPointer<?>  bean = SpringModelSearchers.findBean(model, nameHint);
      // The bean may have been found by an alias, so the name asked for wins over the bean's own name.
      GrLightVariable variable = bean == null ? null : createVariable(manager, nameHint, bean);
      return variable == null ? List.of() : List.of(variable);
    }

    List<GrLightVariable> beans = new ArrayList<>();
    for (SpringBeanPointer<?> pointer : model.getAllCommonBeans()) {
      GrLightVariable variable = createVariable(manager, pointer.getName(), pointer);
      if (variable != null) beans.add(variable);
    }
    return beans;
  }

  private static @Nullable GrLightVariable createVariable(@NotNull PsiManager manager,
                                                          String name,
                                                          @NotNull SpringBeanPointer<?> pointer) {
    if (!pointer.isValid()) return null;

    PsiElement psiElement = pointer.getPsiElement();
    if (psiElement == null) return null;

    PsiType type = TypesUtil.getLeastUpperBound(pointer.getEffectiveBeanTypes().toArray(PsiType.EMPTY_ARRAY), manager);
    return new GrLightVariable(manager, name, type, psiElement);
  }
}

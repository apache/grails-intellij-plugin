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

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.util.Computable;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiField;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.targets.AliasingPsiTarget;
import com.intellij.psi.targets.AliasingPsiTargetMapper;
import com.intellij.spring.model.CommonSpringBean;
import com.intellij.spring.model.SpringBeanPointer;
import com.intellij.spring.model.utils.SpringBeanUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.stubs.GroovyShortNamesCache;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class SpringGrailsTargetMapper implements AliasingPsiTargetMapper {

  @Override
  public @NotNull Set<AliasingPsiTarget> getTargets(final @NotNull PomTarget target) {
    return ApplicationManager.getApplication().runReadAction(new Computable<>() {
      @Override
      public Set<AliasingPsiTarget> compute() {
        CommonSpringBean bean = SpringBeanUtils.getInstance().findBean(target);

        if (bean == null) return Collections.emptySet();

        String name = bean.getBeanName();
        if (name == null) return Collections.emptySet();

        Module module = bean.getModule();
        if (module == null) return Collections.emptySet();

        GlobalSearchScope scope = GlobalSearchScope.moduleWithDependentsScope(module);
        GroovyShortNamesCache cache = GroovyShortNamesCache.getGroovyShortNamesCache(module.getProject());

        Set<AliasingPsiTarget> res = new HashSet<>();

        for (final PsiField psiField : cache.getFieldsByName(name, scope)) {
          SpringBeanPointer<?> pointer = InjectedSpringBeanProvider.getInjectedBean(psiField);
          if (pointer != null && bean.equals(pointer.getSpringBean())) {
            res.add(new AliasingPsiTarget(psiField) {
              @Override
              public @NotNull AliasingPsiTarget setAliasName(@NotNull String newAliasName) {
                psiField.setName(newAliasName);
                return super.setAliasName(newAliasName);
              }
            });
          }
        }

        return res;
      }
    });
  }

}

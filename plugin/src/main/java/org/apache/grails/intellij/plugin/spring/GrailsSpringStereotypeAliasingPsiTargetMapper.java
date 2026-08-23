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

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.project.DumbService;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiTarget;
import com.intellij.psi.targets.AliasingPsiTarget;
import com.intellij.psi.targets.AliasingPsiTargetMapper;
import com.intellij.spring.java.SpringJavaClassInfo;
import com.intellij.spring.model.jam.JamPsiMemberSpringBean;
import com.intellij.spring.model.jam.JamSpringBeanPointer;
import com.intellij.spring.model.jam.stereotype.SpringStereotypeElement;
import com.intellij.spring.model.utils.SpringCommonUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

import java.util.Collections;
import java.util.List;
import java.util.Set;

public final class GrailsSpringStereotypeAliasingPsiTargetMapper implements AliasingPsiTargetMapper {

  @Override
  public @NotNull Set<AliasingPsiTarget> getTargets(final @NotNull PomTarget psiTarget) {
    if (!(psiTarget instanceof GrClassDefinition)) {
      return Collections.emptySet();
    }

    return ReadAction.compute(() -> {
      final PsiClass psiClass = (PsiClass)psiTarget;
      if (psiClass.isInterface()) return Collections.emptySet();

      return DumbService.getInstance(psiClass.getProject()).runReadActionInSmartMode(() -> {
        if (!SpringCommonUtils.isSpringBeanCandidateClassInSpringProject(psiClass)) {
          return Collections.emptySet();
        }

        final SpringJavaClassInfo info = SpringJavaClassInfo.getSpringJavaClassInfo(psiClass);
        final List<JamSpringBeanPointer> stereotypeMappedBeans = info.resolve().getStereotypeMappedBeans();
        for (JamSpringBeanPointer pointer : stereotypeMappedBeans) {
          JamPsiMemberSpringBean<?> stereotypeElement = pointer.getSpringBean();
          if (stereotypeElement instanceof SpringStereotypeElement &&  psiClass.equals(stereotypeElement.getPsiElement())) {
            PsiTarget target = ((SpringStereotypeElement)stereotypeElement).getPsiTarget();
            if (target instanceof AliasingPsiTarget) {
              return Collections.singleton((AliasingPsiTarget)target);
            }
          }
        }
        return Collections.emptySet();
      });
    });
  }
}

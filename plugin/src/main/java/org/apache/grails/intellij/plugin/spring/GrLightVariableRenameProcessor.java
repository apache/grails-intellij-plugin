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

import com.intellij.pom.PomTarget;
import com.intellij.pom.PomTargetPsiElement;
import com.intellij.psi.PsiElement;
import com.intellij.psi.targets.AliasingPsiTarget;
import com.intellij.refactoring.rename.RenameAliasingPomTargetProcessor;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightVariable;

import java.util.Map;

public final class GrLightVariableRenameProcessor extends RenamePsiElementProcessor {
  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    return element instanceof GrLightVariable;
  }

  @Override
  public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
    if (element instanceof GrLightVariable) {
      for (PsiElement target : ((GrLightVariable)element).getDeclarations()) {
      if (target instanceof AliasingPsiTarget) {
        RenameAliasingPomTargetProcessor.prepareAliasingPsiTargetRenaming((AliasingPsiTarget)target, newName, allRenames);
      } else if (target instanceof PomTargetPsiElement) {
        final PomTarget pomTarget = ((PomTargetPsiElement)target).getTarget();
        if (pomTarget instanceof AliasingPsiTarget aliasingPsiTarget) {
          allRenames.put(target, newName);
          RenameAliasingPomTargetProcessor.prepareAliasingPsiTargetRenaming(aliasingPsiTarget, newName, allRenames);
        }
      }
    }}
  }
}

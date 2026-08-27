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

package org.apache.grails.intellij.plugin.references.controller;

import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

import java.util.Map;

public final class ControllerRenameProcessor extends RenamePsiElementProcessor {
  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    if (!(element instanceof GrClassDefinition)) return false;

    return GrailsArtifact.CONTROLLER.isInstance((GrClassDefinition)element);
  }

  @Override
  public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
    String suffix = GrailsArtifact.CONTROLLER.suffix;
    if (!newName.endsWith(suffix) || newName.length() <= suffix.length()) return;

    String newControllerName = GrailsArtifact.CONTROLLER.getArtifactName(newName);

    VirtualFile oldViewsDir = GrailsUtils.getControllerGspDir((PsiClass)element);
    if (oldViewsDir != null) {
      PsiDirectory directory = element.getManager().findDirectory(oldViewsDir);
      if (directory != null) {
        allRenames.put(directory, newControllerName);
      }
    }
  }
}

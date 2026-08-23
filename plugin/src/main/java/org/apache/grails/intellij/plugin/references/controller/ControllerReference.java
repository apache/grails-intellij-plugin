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

import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.icons.AllIcons;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementResolveResult;
import com.intellij.psi.PsiPolyVariantReferenceBase;
import com.intellij.psi.ResolveResult;
import com.intellij.util.ArrayUtilRt;
import com.intellij.util.IncorrectOperationException;
import com.intellij.util.containers.MultiMap;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

import java.util.Collection;
import java.util.Map;

public class ControllerReference extends PsiPolyVariantReferenceBase<PsiElement> {

  public ControllerReference(PsiElement psiElement, boolean soft) {
    super(psiElement, soft);
  }

  public ControllerReference(PsiElement element, TextRange range, boolean soft) {
    super(element, range, soft);
  }

  @Override
  public ResolveResult @NotNull [] multiResolve(boolean incompleteCode) {
    String value = getValue();
    if (value.isEmpty()) return ResolveResult.EMPTY_ARRAY;

    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) return ResolveResult.EMPTY_ARRAY;

    String name = StringUtil.decapitalize(StringUtil.trimEnd(value, "Controller"));
    Collection<GrClassDefinition> controllers = GrailsArtifact.CONTROLLER.getInstances(module, name);

    if (controllers.isEmpty()) return ResolveResult.EMPTY_ARRAY;

    ResolveResult[] res = new ResolveResult[controllers.size()];

    int i = 0;
    for (GrClassDefinition classDefinition : controllers) {
      res[i++] = new PsiElementResolveResult(classDefinition);
    }

    return res;
  }

  @Override
  public Object @NotNull [] getVariants() {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) return ArrayUtilRt.EMPTY_OBJECT_ARRAY;

    MultiMap<String, GrClassDefinition> controllers = GrailsArtifact.CONTROLLER.getInstances(module);

    LookupElement[] res = new LookupElement[controllers.size()];

    int i = 0;
    for (Map.Entry<String, Collection<GrClassDefinition>> entry : controllers.entrySet()) {
      GrTypeDefinition controllerClass = entry.getValue().iterator().next();
      res[i++] = LookupElementBuilder.create(controllerClass, entry.getKey()).withIcon(AllIcons.Nodes.Controller);
    }

    return res;
  }

  @Override
  public PsiElement bindToElement(@NotNull PsiElement element) throws IncorrectOperationException {
    if (element instanceof PsiClass aClass) {
      if (GrailsArtifact.CONTROLLER.isInstance(aClass)) {
        String artifactName = GrailsArtifact.CONTROLLER.getArtifactName(aClass);
        if (getValue().equals(artifactName)) {
          return getElement();
        }
      }
    }
    return super.bindToElement(element);
  }

  @Override
  public PsiElement handleElementRename(@NotNull String newElementName) throws IncorrectOperationException {
    if (!newElementName.endsWith(GrailsArtifact.CONTROLLER.suffix)) {
      return getElement();
    }

    return super.handleElementRename(GrailsArtifact.CONTROLLER.getArtifactName(newElementName));
  }

}

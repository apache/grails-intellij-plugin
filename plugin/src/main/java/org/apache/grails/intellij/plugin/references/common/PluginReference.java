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

package org.apache.grails.intellij.plugin.references.common;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.psi.PsiReferenceProvider;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.config.GrailsFramework;
import org.apache.grails.intellij.plugin.util.GrailsUtils;

public class PluginReference extends PsiReferenceBase<PsiElement> {

  private final Module myModule;

  public PluginReference(@NotNull Module module, PsiElement element, boolean soft) {
    super(element, soft);
    myModule = module;
  }

  @Override
  public PsiElement resolve() {
    String text = getValue();
    VirtualFile virtualFile = GrailsFramework.getInstance().findPluginRoot(myModule, text, false);
    if (virtualFile == null) return null;
    return getElement().getManager().findDirectory(virtualFile);
  }

  @Override
  public Object @NotNull [] getVariants() {
    return GrailsUtils.createPluginVariants(myModule, false);
  }

  public static class Provider extends PsiReferenceProvider {
    @Override
    public PsiReference @NotNull [] getReferencesByElement(@NotNull PsiElement element, @NotNull ProcessingContext context) {
      final Module module = ModuleUtilCore.findModuleForPsiElement(element);
      if (module == null) return PsiReference.EMPTY_ARRAY;

      return new PsiReference[]{new PluginReference(module, element, false)};
    }
  }
}

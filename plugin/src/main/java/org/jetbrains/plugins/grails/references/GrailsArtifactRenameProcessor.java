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

package org.jetbrains.plugins.grails.references;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.refactoring.rename.RenamePsiElementProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.tests.GrailsTestUtils;
import org.jetbrains.plugins.grails.util.GrailsArtifact;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrClassDefinition;

import java.util.Map;

public final class GrailsArtifactRenameProcessor extends RenamePsiElementProcessor {

  @Override
  public boolean canProcessElement(@NotNull PsiElement element) {
    if (!(element instanceof GrClassDefinition)) return false;
    return GrailsArtifact.getType((PsiClass)element) != null;
  }

  @Override
  public void prepareRenaming(@NotNull PsiElement element, @NotNull String newName, @NotNull Map<PsiElement, String> allRenames) {
    Module module = ModuleUtilCore.findModuleForPsiElement(element);
    if (module == null) return;

    PsiClass aClass = (PsiClass)element;

    String qname = aClass.getQualifiedName();
    if (qname == null) return;

    GrailsArtifact artifact = GrailsArtifact.getType(aClass);
    assert artifact != null;

    if (!newName.endsWith(artifact.suffix)) return;

    String oldName = aClass.getName();

    for (PsiClass psiClass : GrailsTestUtils.getTestsForArtifact(aClass, true)) {
      String name = psiClass.getName();
      if (name.startsWith(oldName)) {
        allRenames.put(psiClass, newName + name.substring(oldName.length()));
      }
    }

    GlobalSearchScope scope = GlobalSearchScope.moduleScope(module).intersectWith(
      GlobalSearchScopesCore.projectTestScope(module.getProject()));

    JavaPsiFacade facade = JavaPsiFacade.getInstance(element.getProject());

    for (String suffix : new String[]{"Test", "Tests", "Spec", "Specification"}) {
      for (PsiClass psiClass : facade.findClasses(qname + suffix, scope)) {
        allRenames.put(psiClass, newName + suffix);
      }
    }
  }
}

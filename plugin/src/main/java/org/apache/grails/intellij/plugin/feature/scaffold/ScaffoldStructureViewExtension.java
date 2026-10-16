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
package org.apache.grails.intellij.plugin.feature.scaffold;

import com.intellij.icons.AllIcons;
import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.structureView.StructureViewExtension;
import com.intellij.ide.structureView.StructureViewFactoryEx;
import com.intellij.ide.structureView.StructureViewTreeElement;
import com.intellij.ide.util.InheritedMembersNodeProvider;
import com.intellij.ide.util.treeView.smartTree.TreeElement;
import com.intellij.navigation.ItemPresentation;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.ui.SimpleTextAttributes;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

import java.util.List;

/**
 * Lists the methods {@code @Scaffold} gives a controller or service under its node in the Structure
 * view, greyed and located in the base class, without the user having to turn on "Show Inherited".
 * When that toggle is on the platform already lists them (the superclass is set by
 * {@link ScaffoldTransformationSupport}), so this adds nothing then.
 */
public final class ScaffoldStructureViewExtension implements StructureViewExtension {

  @Override
  public Class<? extends PsiElement> getType() {
    return GrTypeDefinition.class;
  }

  @Override
  public StructureViewTreeElement[] getChildren(PsiElement parent) {
    if (!(parent instanceof GrTypeDefinition clazz)) return StructureViewTreeElement.EMPTY_ARRAY;
    if (StructureViewFactoryEx.getInstanceEx(clazz.getProject()).isActionActive(InheritedMembersNodeProvider.ID)) {
      return StructureViewTreeElement.EMPTY_ARRAY;
    }
    Scaffolding.ScaffoldInfo info = Scaffolding.resolve(clazz);
    if (info == null) return StructureViewTreeElement.EMPTY_ARRAY;

    List<PsiMethod> members = Scaffolding.inheritedMembers(clazz, info);
    StructureViewTreeElement[] result = new StructureViewTreeElement[members.size()];
    for (int i = 0; i < result.length; i++) {
      result[i] = new ScaffoldedMethodElement(members.get(i), info);
    }
    return result;
  }

  @Override
  public @Nullable Object getCurrentEditorElement(Editor editor, PsiElement parent) {
    return null;
  }

  /** A scaffolded method: shown greyed, located in the base that declares it, navigating there. */
  static final class ScaffoldedMethodElement implements StructureViewTreeElement {
    private final PsiMethod myMethod;
    private final Scaffolding.ScaffoldInfo myInfo;

    ScaffoldedMethodElement(@NotNull PsiMethod method, @NotNull Scaffolding.ScaffoldInfo info) {
      myMethod = method;
      myInfo = info;
    }

    @Override
    public PsiMethod getValue() {
      return myMethod;
    }

    @Override
    public @NotNull ItemPresentation getPresentation() {
      PresentationData presentation = new PresentationData();
      presentation.addText(Scaffolding.signature(myMethod, myInfo), SimpleTextAttributes.GRAYED_ATTRIBUTES);
      presentation.setPresentableText(Scaffolding.signature(myMethod, myInfo));
      presentation.setIcon(AllIcons.Nodes.Method);
      PsiElement containingClass = myMethod.getContainingClass();
      if (containingClass instanceof com.intellij.psi.PsiClass psiClass) {
        presentation.setLocationString(psiClass.getName());
      }
      return presentation;
    }

    @Override
    public TreeElement @NotNull [] getChildren() {
      return TreeElement.EMPTY_ARRAY;
    }

    @Override
    public void navigate(boolean requestFocus) {
      myMethod.navigate(requestFocus);
    }

    @Override
    public boolean canNavigate() {
      return myMethod.canNavigate();
    }

    @Override
    public boolean canNavigateToSource() {
      return myMethod.canNavigateToSource();
    }
  }
}

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

package org.jetbrains.plugins.grails.projectView.nodes;

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.grails.artefact.impl.controllers.FunctionsKt;
import org.jetbrains.plugins.grails.structure.GrailsApplication;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.GrTypeDefinition;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

public class GrailsControllerNode extends ClassTreeNode {

  private volatile GrailsApplication grailsApplication;

  public GrailsControllerNode(@NotNull PsiClass clazz, @NotNull ViewSettings settings) {
    super(clazz.getProject(), clazz, settings);
  }

  public @NotNull GrailsApplication getGrailsApplication() {
    GrailsApplication result = grailsApplication;
    if (result == null) {
      result = TreeNodeUtil.findNotNullValueOfType(this, GrailsApplication.class);
      grailsApplication = result;
    }
    return result;
  }

  @Override
  public @Nullable Collection<AbstractTreeNode<?>> getChildrenImpl() {
    if (!getSettings().isShowMembers()) return null;
    if (!(getValue() instanceof GrTypeDefinition clazz)) return null;

    Collection<AbstractTreeNode<?>> result = new ArrayList<>();
    for (Map.Entry<String, PsiMember> action : FunctionsKt.getActions(clazz, getGrailsApplication()).entrySet()) {
      result.add(new GrailsActionNode(action.getKey(), action.getValue(), getSettings()));
    }
    return result;
  }
}

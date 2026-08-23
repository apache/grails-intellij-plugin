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

package org.apache.grails.intellij.plugin.projectView.nodes;

import com.intellij.ide.projectView.PresentationData;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.BasePsiMemberNode;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiMember;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.util.GrailsUtils;

import java.util.ArrayList;
import java.util.Collection;

public class GrailsActionNode extends BasePsiMemberNode<PsiMember> {

  private final String actionName;

  public GrailsActionNode(@NotNull String actionName, @NotNull PsiMember action, @NotNull ViewSettings settings) {
    super(action.getProject(), action, settings);
    this.actionName = actionName;
  }

  public @NotNull String getActionName() {
    return actionName;
  }

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> getChildrenImpl() {
    Collection<AbstractTreeNode<?>> result = new ArrayList<>();
    for (PsiFile view : GrailsUtils.getViewPsiByAction(getValue())) {
      result.add(new PsiFileNode(getProject(), view, getSettings()));
    }
    return result;
  }

  @Override
  protected void updateImpl(@NotNull PresentationData data) {
    data.setPresentableText(actionName);
    data.setIcon(GroovyMvcIcons.Action_method);
  }
}

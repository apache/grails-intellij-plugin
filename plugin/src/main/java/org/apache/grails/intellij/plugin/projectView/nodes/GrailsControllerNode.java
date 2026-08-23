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

import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.projectView.impl.nodes.ClassTreeNode;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiMember;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.artefact.impl.controllers.ControllerActions;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;
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
    for (Map.Entry<String, PsiMember> action : ControllerActions.getActions(clazz, getGrailsApplication()).entrySet()) {
      result.add(new GrailsActionNode(action.getKey(), action.getValue(), getSettings()));
    }
    return result;
  }
}

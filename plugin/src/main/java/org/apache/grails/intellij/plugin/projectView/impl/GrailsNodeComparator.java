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

package org.apache.grails.intellij.plugin.projectView.impl;

import com.intellij.ide.projectView.impl.GroupByTypeComparator;
import com.intellij.ide.projectView.impl.nodes.PsiFileNode;
import com.intellij.ide.util.treeView.NodeDescriptor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.projectView.GrailsPluginsNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsApplicationNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsArtefactHandlerNode;
import org.apache.grails.intellij.plugin.projectView.nodes.GrailsPsiDirectoryNode;
import org.apache.grails.intellij.plugin.projectView.nodes.OldGrailsPluginsNode;
import org.apache.grails.intellij.plugin.structure.GrailsApplication;

import java.util.Comparator;

public final class GrailsNodeComparator implements Comparator<NodeDescriptor<?>> {

  private final GroupByTypeComparator delegate;

  public GrailsNodeComparator(@NotNull Project project, @NotNull String id) {
    this.delegate = new GroupByTypeComparator(project, id);
  }

  @Override
  public int compare(NodeDescriptor<?> left, NodeDescriptor<?> right) {
    if (right instanceof OldGrailsPluginsNode || right instanceof GrailsPluginsNode) return -1;
    if (left instanceof OldGrailsPluginsNode || left instanceof GrailsPluginsNode) return 1;

    if (left instanceof GrailsApplicationNode leftApp && right instanceof GrailsApplicationNode rightApp) {
      GrailsApplication leftValue = leftApp.getValue();
      GrailsApplication rightValue = rightApp.getValue();
      return StringUtil.naturalCompare(leftValue != null ? leftValue.getName() : null,
                                       rightValue != null ? rightValue.getName() : null);
    }

    if (left instanceof GrailsArtefactHandlerNode leftHandler && right instanceof GrailsArtefactHandlerNode rightHandler) {
      return leftHandler.getValue().getWeight() - rightHandler.getValue().getWeight();
    }
    if (left instanceof GrailsArtefactHandlerNode) return -1;
    if (right instanceof GrailsArtefactHandlerNode) return 1;

    if (left instanceof GrailsPsiDirectoryNode leftDir && right instanceof GrailsPsiDirectoryNode rightDir) {
      return leftDir.getNodeWeight() - rightDir.getNodeWeight();
    }

    if (right instanceof PsiFileNode rightFile && !(left instanceof PsiFileNode)
        && rightFile.getParent() instanceof GrailsApplicationNode) {
      return -1;
    }
    if (left instanceof PsiFileNode leftFile && !(right instanceof PsiFileNode)
        && leftFile.getParent() instanceof GrailsApplicationNode) {
      return 1;
    }

    return delegate.compare(left, right);
  }
}

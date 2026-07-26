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

package org.jetbrains.plugins.grails.projectView;

import com.intellij.ide.projectView.TreeStructureProvider;
import com.intellij.ide.projectView.ViewSettings;
import com.intellij.ide.util.treeView.AbstractTreeNode;
import com.intellij.openapi.actionSystem.CommonDataKeys;
import com.intellij.openapi.actionSystem.DataSink;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.actions.GrailsActionUtil;
import org.jetbrains.plugins.grails.artefact.api.GrailsArtefactHandler;
import org.jetbrains.plugins.grails.projectView.nodes.GrailsApplicationNode;
import org.jetbrains.plugins.grails.projectView.nodes.GrailsArtefactHandlerNode;
import org.jetbrains.plugins.grails.projectView.nodes.GrailsArtefactPackageNode;
import org.jetbrains.plugins.grails.projectView.nodes.TreeNodeUtil;
import org.jetbrains.plugins.grails.structure.GrailsApplication;

import java.util.Collection;
import java.util.Iterator;

public final class GrailsTreeStructureProvider implements TreeStructureProvider {

  @Override
  public @NotNull Collection<AbstractTreeNode<?>> modify(@NotNull AbstractTreeNode<?> parent,
                                                         @NotNull Collection<AbstractTreeNode<?>> children,
                                                         ViewSettings settings) {
    return children;
  }

  @Override
  public void uiDataSnapshot(@NotNull DataSink sink, @NotNull Collection<? extends AbstractTreeNode<?>> selection) {
    AbstractTreeNode<?> single = singleOrNull(selection);
    if (single == null) return;

    sink.lazy(CommonDataKeys.VIRTUAL_FILE, () -> {
      if (single instanceof GrailsApplicationNode applicationNode) {
        return applicationNode.getValue().getRoot();
      }
      if (single instanceof GrailsArtefactHandlerNode handlerNode) {
        return handlerNode.getValue().getDirectory(handlerNode.getGrailsApplication());
      }
      return null;
    });
    sink.lazy(GrailsActionUtil.GRAILS_APPLICATION, () -> TreeNodeUtil.findValueOfType(single, GrailsApplication.class));
    sink.lazy(GrailsActionUtil.GRAILS_ARTEFACT_HANDLER, () -> TreeNodeUtil.findValueOfType(single, GrailsArtefactHandler.class));
    sink.lazy(GrailsActionUtil.GRAILS_ARTEFACT_PACKAGE,
              () -> single instanceof GrailsArtefactPackageNode<?> packageNode ? packageNode.getPackageFqn() : null);
  }

  private static AbstractTreeNode<?> singleOrNull(@NotNull Collection<? extends AbstractTreeNode<?>> selection) {
    if (selection.size() != 1) return null;
    Iterator<? extends AbstractTreeNode<?>> iterator = selection.iterator();
    return iterator.hasNext() ? iterator.next() : null;
  }
}

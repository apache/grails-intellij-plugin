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

package org.apache.grails.intellij.plugin.perspectives.graph;

import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.base.Graph;
import com.intellij.openapi.graph.builder.DeleteProvider;
import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.components.BasicGraphPresentationModel;
import com.intellij.openapi.graph.services.GraphLayoutService;
import com.intellij.openapi.graph.services.GraphNodeRealizerService;
import com.intellij.openapi.graph.view.Arrow;
import com.intellij.openapi.graph.view.EdgeRealizer;
import com.intellij.openapi.graph.view.EditMode;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.graph.view.LineType;
import com.intellij.openapi.graph.view.NodeRealizer;
import com.intellij.openapi.graph.view.PolyLineEdgeRealizer;
import com.intellij.ui.JBColor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.perspectives.DomainClassNodeRenderer;
import org.apache.grails.intellij.plugin.perspectives.delete.RelationDeleteProvider;

public class DomainClassDependencyPresentation extends BasicGraphPresentationModel<DomainClassNode, DomainClassRelationsInfo> {
  private final DomainClassesRelationsDataModel myDataModel;
  private DomainClassNodeRenderer myNodeCellRenderer;

  public DomainClassDependencyPresentation(Graph graph, DomainClassesRelationsDataModel dataModel) {
    super(graph);
    myDataModel = dataModel;

    final var settings = getSettings();
    settings.setCurrentLayouter(GraphLayoutService.getInstance().getChannelLayouter());
    settings.setFitContentAfterLayout(true);
    settings.setShowEdgeLabels(true);
  }

  @Override
  public @NotNull NodeRealizer getNodeRealizer(final @Nullable DomainClassNode node) {
    if (myNodeCellRenderer == null) {
      GraphBuilder<DomainClassNode, DomainClassRelationsInfo> builder = getGraphBuilder();
      DataModelAndSelectionModificationTracker tracker = new DataModelAndSelectionModificationTracker(myDataModel.getProject());
      myNodeCellRenderer = new DomainClassNodeRenderer(builder, tracker, myDataModel);
    }
    return GraphNodeRealizerService.getInstance().createGenericNodeRealizer("DomainClassNodeRenderer", myNodeCellRenderer);
  }

  @Override
  public @NotNull EdgeRealizer getEdgeRealizer(DomainClassRelationsInfo edge) {
    final PolyLineEdgeRealizer edgeRealizer = GraphManager.getGraphManager().createPolyLineEdgeRealizer();
    edgeRealizer.setLineType(LineType.LINE_1);

    switch (edge.getRelation()) {
      case UNKNOWN -> {
        edgeRealizer.setLineColor(JBColor.GREEN.darker());
        edgeRealizer.setTargetArrow(Arrow.STANDARD);
        edgeRealizer.setSourceArrow(Arrow.NONE);
      }
      case STRONG -> {
        edgeRealizer.setLineColor(JBColor.CYAN.darker());
        edgeRealizer.setTargetArrow(Arrow.STANDARD);
        edgeRealizer.setSourceArrow(Arrow.NONE);
      }
      case BELONGS_TO -> {
        edgeRealizer.setLineColor(JBColor.GREEN.darker());
        edgeRealizer.setTargetArrow(Arrow.STANDARD);
        edgeRealizer.setSourceArrow(Arrow.NONE);
      }
      case HAS_MANY -> {
        edgeRealizer.setLineColor(JBColor.BLUE.darker());
        edgeRealizer.setTargetArrow(Arrow.STANDARD);
        edgeRealizer.setSourceArrow(Arrow.NONE);
      }
      default -> {
        edgeRealizer.setLineType(LineType.LINE_1);
        edgeRealizer.setLineColor(JBColor.GRAY);
        edgeRealizer.setArrow(Arrow.STANDARD);
      }
    }

    return edgeRealizer;
  }

  @Override
  public @Nullable String getEdgeTooltip(final DomainClassRelationsInfo edge) {
    return edge.getEdgeLabel();
  }

  @Override
  public void customizeSettings(final @NotNull Graph2DView view, final @NotNull EditMode editMode) {
    editMode.allowEdgeCreation(true);
    editMode.allowBendCreation(false);
    view.setFitContentOnResize(true);
  }

  @Override
  public boolean editNode(DomainClassNode domainClassNode) {
    domainClassNode.getTypeDefinition().navigate(true);
    return true;
  }

  @Override
  public @Nullable DeleteProvider getDeleteProvider() {
    return new RelationDeleteProvider(myDataModel.getProject());
  }
}

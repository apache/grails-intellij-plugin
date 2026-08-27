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

package org.apache.grails.intellij.plugin.perspectives;

import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.ActionToolbar;
import com.intellij.openapi.actionSystem.DataSink;
import com.intellij.openapi.actionSystem.PlatformCoreDataKeys;
import com.intellij.openapi.actionSystem.UiDataProvider;
import com.intellij.openapi.graph.GraphManager;
import com.intellij.openapi.graph.builder.GraphBuilder;
import com.intellij.openapi.graph.builder.GraphBuilderFactory;
import com.intellij.openapi.graph.builder.actions.AbstractGraphAction;
import com.intellij.openapi.graph.view.Graph2D;
import com.intellij.openapi.graph.view.Graph2DView;
import com.intellij.openapi.graph.view.Overview;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiTreeChangeAdapter;
import com.intellij.psi.PsiTreeChangeEvent;
import com.intellij.util.xml.ui.Committable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.perspectives.graph.DomainClassDependencyPresentation;
import org.apache.grails.intellij.plugin.perspectives.graph.DomainClassNode;
import org.apache.grails.intellij.plugin.perspectives.graph.DomainClassRelationsInfo;
import org.apache.grails.intellij.plugin.perspectives.graph.DomainClassesRelationsDataModel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

public class PerspectiveFileEditorComponent extends JPanel implements UiDataProvider, Committable {
  private static final String HELP_ID = "reference.persistencediagram";

  private final GraphBuilder<DomainClassNode, DomainClassRelationsInfo> myBuilder;
  private final DomainClassesRelationsDataModel myDataModel;
  private final Project myProject;
  private final PsiTreeChangeAdapter myListener;

  public PerspectiveFileEditorComponent(@Nullable VirtualFile domainDirectory, Project project) {
    myProject = project;

    final Graph2D graph = GraphManager.getGraphManager().createGraph2D();
    final Graph2DView view = GraphManager.getGraphManager().createGraph2DView();

    myDataModel = new DomainClassesRelationsDataModel(domainDirectory, myProject);

    DomainClassDependencyPresentation presentationModel = new DomainClassDependencyPresentation(graph, myDataModel);

    myBuilder = GraphBuilderFactory.getInstance(myProject).createGraphBuilder(graph, view, myDataModel, presentationModel);
    JComponent graphComponent = myBuilder.getView().getJComponent();

    setLayout(new BorderLayout());

    ActionToolbar toolbar = ActionManager.getInstance().createActionToolbar(
      ActionPlaces.TOOLBAR, AbstractGraphAction.getCommonToolbarActions(), true);
    toolbar.setTargetComponent(graphComponent);

    add(toolbar.getComponent(), BorderLayout.NORTH);
    add(graphComponent, BorderLayout.CENTER);

    myListener = new PsiTreeChangeAdapter() {
      @Override
      public void childrenChanged(@NotNull PsiTreeChangeEvent psiTreeChangeEvent) {
        update();
      }

      @Override
      public void childRemoved(@NotNull PsiTreeChangeEvent event) {
        update();
      }

      @Override
      public void childAdded(@NotNull PsiTreeChangeEvent event) {
        update();
      }

      @Override
      public void childReplaced(@NotNull PsiTreeChangeEvent event) {
        update();
      }
    };

    PsiManager.getInstance(myProject).addPsiTreeChangeListener(myListener, this);

    Disposer.register(this, myBuilder);
    myBuilder.initialize();
  }

  private void update() {
    if (isShowing()) {
      myBuilder.queueUpdate();
    }
  }

  @Override
  public void uiDataSnapshot(@NotNull DataSink sink) {
    sink.set(PlatformCoreDataKeys.HELP_ID, HELP_ID);
  }

  @Override
  public void commit() {
  }

  @Override
  public void reset() {
    myBuilder.updateGraph();
  }

  @Override
  public void dispose() {
  }

  public Overview getOverview() {
    return GraphManager.getGraphManager().createOverview(myBuilder.getView());
  }

  public DomainClassesRelationsDataModel getDataModel() {
    return myDataModel;
  }

  public Project getProject() {
    return myProject;
  }

  public GraphBuilder<DomainClassNode, DomainClassRelationsInfo> getBuilder() {
    return myBuilder;
  }
}
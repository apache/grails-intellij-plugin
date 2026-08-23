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

package org.apache.grails.intellij.plugin.util;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.markup.GutterIconRenderer;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.NlsContexts.Tooltip;
import com.intellij.pom.Navigatable;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.Icon;

public class ReferenceGutterIconRenderer extends GutterIconRenderer implements DumbAware {

  private final Navigatable myElementToNavigate;

  private final Icon myIcon;

  private final @Tooltip String myTooltip;

  public ReferenceGutterIconRenderer(@Nullable Navigatable elementToNavigate, @NotNull Icon icon) {
    this(elementToNavigate, icon, null);
  }

  public ReferenceGutterIconRenderer(@Nullable Navigatable elementToNavigate, @NotNull Icon icon, @Nullable @Tooltip String tooltip) {
    myElementToNavigate = elementToNavigate;
    myIcon = icon;
    myTooltip = tooltip;
  }

  @Override
  public String getTooltipText() {
    return myTooltip;
  }

  @Override
  public AnAction getClickAction() {
    return new AnAction() {
      @Override
      public void actionPerformed(@NotNull AnActionEvent e) {
        if (myElementToNavigate != null) {
          myElementToNavigate.navigate(true);
        }
      }
    };
  }

  @Override
  public boolean isNavigateAction() {
    return true;
  }

  @Override
  public @NotNull Icon getIcon() {
    return myIcon;
  }

  @Override
  public int hashCode() {
    return myElementToNavigate == null ? 1 : myElementToNavigate.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ReferenceGutterIconRenderer
           && Comparing.equal(myElementToNavigate, ((ReferenceGutterIconRenderer)obj).myElementToNavigate)
           && Comparing.equal(myIcon, ((ReferenceGutterIconRenderer)obj).getIcon());
  }
}

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

package org.apache.grails.intellij.plugin.spring;

import com.intellij.codeInsight.daemon.LineMarkerInfo;
import com.intellij.codeInsight.daemon.LineMarkerProviderDescriptor;
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiElement;
import com.intellij.spring.SpringApiIcons;
import com.intellij.spring.model.SpringBeanPointer;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GrailsBundle;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrField;

import javax.swing.Icon;
import java.util.Collection;
import java.util.List;

final class SpringInjectionAnnotator extends LineMarkerProviderDescriptor {
  @Override
  public String getId() {
    return "GrailsSpringInjectedBean";
  }

  @Override
  public @NotNull Icon getIcon() {
    return SpringApiIcons.Gutter.SpringBean;
  }

  @Override
  public String getName() {
    return GrailsBundle.message("gutter.name.grails.spring.beans");
  }

  @Override
  public @Nullable LineMarkerInfo<?> getLineMarkerInfo(@NotNull PsiElement element) {
    return null;
  }

  @Override
  public void collectSlowLineMarkers(@NotNull List<? extends PsiElement> elements, @NotNull Collection<? super LineMarkerInfo<?>> result) {
    for (PsiElement element : elements) {
      PsiElement parent = element.getParent();
      if (parent instanceof GrField && element == ((GrField)parent).getNameIdentifierGroovy()) {
        SpringBeanPointer<?> bean = InjectedSpringBeanProvider.getInjectedBean((GrField)parent);
        if (bean == null) return;

        PsiElement navigateElement = bean.getPsiElement();
        if (!(navigateElement instanceof Navigatable)) return;

        result.add(
          NavigationGutterIconBuilder.create(SpringApiIcons.Gutter.ShowAutowiredDependencies,
                                             GrailsBundle.message("grails.gutter.spring.nav.group"))
            .setTarget(navigateElement)
            .setTooltipText(GrailsBundle.message("tooltip.injected.spring.bean", bean.getName())).createLineMarkerInfo(element)
        );
      }
    }
  }
}

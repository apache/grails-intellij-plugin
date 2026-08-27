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

package org.apache.grails.intellij.plugin.references.controller;

import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiReferenceBase;
import com.intellij.util.Function;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.apache.grails.intellij.plugin.GroovyMvcIcons;
import org.apache.grails.intellij.plugin.util.GrailsUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ActionReference extends PsiReferenceBase<PsiElement> implements Function<PsiElement, Map<String, PsiMethod>> {

  private final Function<? super PsiElement, ? extends Map<String, PsiMethod>> myControllerResolver;

  private final String myControllerName;

  private Map<String, PsiMethod> myActions;

  public ActionReference(PsiElement element,
                         TextRange range,
                         boolean soft,
                         Function<? super PsiElement, ? extends Map<String, PsiMethod>> controllerResolver) {
    super(element, range, soft);

    TextRange textRange = trimExtension(range);
    setRangeInElement(textRange);

    myControllerResolver = controllerResolver;
    myControllerName = null;
  }

  public ActionReference(PsiElement element, boolean soft, Function<? super PsiElement, ? extends Map<String, PsiMethod>> controllerResolver) {
    super(element, soft);
    myControllerResolver = controllerResolver;
    myControllerName = null;
  }

  public ActionReference(PsiElement element, boolean soft, @NotNull String controllerName) {
    super(element, soft);
    myControllerResolver = this;
    myControllerName = controllerName;
  }

  private TextRange trimExtension(TextRange defaultRange) {
    String elementText = getElement().getText();

    String value = defaultRange.substring(elementText);

    int dotIndex = value.lastIndexOf('.');
    if (dotIndex >= 0) {
      return TextRange.from(defaultRange.getStartOffset(), dotIndex);
    }

    return defaultRange;
  }

  @Override
  protected TextRange calculateDefaultRangeInElement() {
    TextRange defaultRange = super.calculateDefaultRangeInElement();
    defaultRange = trimExtension(defaultRange);
    return defaultRange;
  }

  @Override
  public PsiElement resolve() {
    String value = getValue();
    if (value.isEmpty()) return null;

    return GrailsUtils.toField(getActions().get(value));
  }

  public static LookupElementBuilder[] createLookupItems(Collection<String> actionNames) {
    LookupElementBuilder[] res = new LookupElementBuilder[actionNames.size()];

    int i = 0;

    for (String actionName : actionNames) {
      res[i++] = LookupElementBuilder.create(actionName).withIcon(GroovyMvcIcons.Action_method);
    }

    return res;
  }

  @Override
  public Object @NotNull [] getVariants() {
    return createLookupItems(getActions().keySet());
  }

  private Map<String, PsiMethod> getActions() {
    Map<String, PsiMethod> res = myActions;
    if (res == null) {
      res = myControllerResolver.fun(getElement());
      myActions = res;
    }

    return res;
  }

  public @Nullable String getControllerName() {
    return myControllerName;
  }

  @Override
  public Map<String, PsiMethod> fun(PsiElement psiElement) {
    Module module = ModuleUtilCore.findModuleForPsiElement(getElement());
    if (module == null) return Collections.emptyMap();

    return GrailsUtils.getControllerActions(myControllerName, module);
  }
}

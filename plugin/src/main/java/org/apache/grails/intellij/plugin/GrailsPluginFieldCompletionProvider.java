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

package org.apache.grails.intellij.plugin;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.lookup.EqTailType;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.lookup.TailTypeDecorator;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ProcessingContext;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.util.GrailsUtils;

public class GrailsPluginFieldCompletionProvider extends CompletionProvider<CompletionParameters> {

  // #CHECK# Find usages of GrailsClassUtils.getPropertyOrStaticPropertyOrFieldValue() in DefaultGrailsPlugin
  public static final String[] VARIANTS = {
    "autor", "title", "description", "grailsVersion", "version", "documentation", "pluginExcludes", "dependsOn", "loadAfter",
    "watchedResources", "artefacts", "doWithSpring", "doWithDynamicMethods", "doWithApplicationContext", "onChange",
    "onConfigChangeListener", "onShutdownListener", "influences", "observe", "scopes", "environments", "evict", "loadBefore", "status",
    "providedArtefacts", "typeFilters"
  };

  @Override
  protected void addCompletions(@NotNull CompletionParameters parameters,
                                @NotNull ProcessingContext context,
                                @NotNull CompletionResultSet result) {
    PsiFile file = parameters.getOriginalFile();
    if (!file.getName().endsWith("GrailsPlugin.groovy")) return;

    PsiClass aClass = PsiTreeUtil.getParentOfType(parameters.getPosition(), PsiClass.class);

    if (!GrailsUtils.isGrailsPluginClass(aClass)) return;
    assert aClass != null;

    for (String variant : VARIANTS) {
      if (aClass.findFieldByName(variant, false) == null) {
        result.addElement(TailTypeDecorator.withTail(LookupElementBuilder.create(variant), EqTailType.INSTANCE));
      }
    }
  }
}

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

package org.apache.grails.intellij.module.i18n;

import com.intellij.codeInspection.i18n.JavaI18nizeQuickFixDialog;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.TestOnly;
import org.apache.grails.intellij.plugin.util.GrailsUtils;
import org.jetbrains.uast.UExpression;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

abstract class GrailsI18nizeQuickFixDialog extends JavaI18nizeQuickFixDialog<UExpression> {

  GrailsI18nizeQuickFixDialog(@NotNull Project project,
                              final @NotNull PsiFile context,
                              @NotNull String defaultPropertyValue) {
    super(project, context, null, defaultPropertyValue, null, false, true);
  }

  protected abstract @Nullable String getArgs();

  @Override
  protected void addAdditionalAttributes(Map<String, String> attributes) {
    attributes.put("ARGS_KEY", getArgs());
    attributes.put(PROPERTY_VALUE_ATTR, myDefaultPropertyValue);
  }

  @TestOnly
  public String getDefaultPropertyValue() {
    return myDefaultPropertyValue;
  }

  @Override
  protected List<String> defaultSuggestPropertiesFiles() {
    VirtualFile i18nFile = GrailsUtils.findI18nDirectory(myContext);
    if (i18nFile == null) return super.defaultSuggestPropertiesFiles();

    List<String> res = new ArrayList<>();

    for (VirtualFile virtualFile : i18nFile.getChildren()) {
      if (virtualFile.getName().endsWith(".properties")) {
        res.add(virtualFile.getPath());
      }
    }

    Collections.sort(res);

    return res;
  }

  @Override
  protected abstract @NotNull String getTemplateName();

}

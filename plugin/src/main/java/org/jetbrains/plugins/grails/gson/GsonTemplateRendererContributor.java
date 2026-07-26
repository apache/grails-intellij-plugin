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
package org.jetbrains.plugins.grails.gson;

import com.intellij.psi.CommonClassNames;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiType;
import com.intellij.psi.ResolveState;
import com.intellij.psi.scope.PsiScopeProcessor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.impl.synthetic.GrLightMethodBuilder;
import org.jetbrains.plugins.groovy.lang.resolve.NonCodeMembersContributor;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtil;
import org.jetbrains.plugins.groovy.lang.resolve.ResolveUtilKt;

public final class GsonTemplateRendererContributor extends NonCodeMembersContributor {

  private static final String JSON_UNESCAPED = "grails.plugin.json.builder.JsonOutput.JsonUnescaped";

  @Override
  protected String getParentClassName() {
    return "grails.plugin.json.view.api.internal.TemplateRenderer";
  }

  @Override
  public void processDynamicElements(@NotNull PsiType qualifierType,
                                     @NotNull PsiScopeProcessor processor,
                                     @NotNull PsiElement place,
                                     @NotNull ResolveState state) {
    if (!ResolveUtilKt.shouldProcessMethods(processor)) return;
    String name = ResolveUtil.getNameHint(processor);
    if (name == null) return;

    // render(value) and render(var, collection); both are synthesised for whatever name the
    // resolver is asking about.
    GrLightMethodBuilder single = new GrLightMethodBuilder(place.getManager(), name);
    single.addParameter("value", CommonClassNames.JAVA_LANG_OBJECT);
    single.setReturnType(JSON_UNESCAPED, place.getResolveScope());
    if (!processor.execute(single, state)) return;

    GrLightMethodBuilder collection = new GrLightMethodBuilder(place.getManager(), name);
    collection.addParameter("var", CommonClassNames.JAVA_LANG_OBJECT);
    collection.addParameter("collection", CommonClassNames.JAVA_LANG_ITERABLE);
    collection.setReturnType(JSON_UNESCAPED, place.getResolveScope());
    processor.execute(collection, state);
  }
}

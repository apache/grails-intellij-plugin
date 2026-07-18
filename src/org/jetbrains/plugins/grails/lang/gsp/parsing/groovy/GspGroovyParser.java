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

package org.jetbrains.plugins.grails.lang.gsp.parsing.groovy;

import com.intellij.lang.ASTNode;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilder.Marker;
import com.intellij.lang.PsiParser;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.grails.lang.gsp.parsing.GspGroovyElementTypes;

public class GspGroovyParser implements PsiParser, GspGroovyElementTypes {

  @Override
  public @NotNull ASTNode parse(@NotNull IElementType root, @NotNull PsiBuilder builder) {
    Marker fileMarker = builder.mark();
    Marker classMarker = builder.mark();
    Marker methodMarker = builder.mark();

    new GspAwareGroovyParser().parseLight(GSP_RUN_BLOCK, builder);

    methodMarker.done(GSP_RUN_METHOD);
    classMarker.done(GSP_CLASS);
    fileMarker.done(root);

    return builder.getTreeBuilt();
  }
}

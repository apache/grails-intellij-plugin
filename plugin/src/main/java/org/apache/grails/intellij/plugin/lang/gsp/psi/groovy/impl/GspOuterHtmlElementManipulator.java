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

package org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.impl;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.AbstractElementManipulator;
import com.intellij.psi.impl.source.tree.Factory;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.impl.source.tree.SharedImplUtil;
import com.intellij.util.CharTable;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.GspTokenTypesEx;

public final class GspOuterHtmlElementManipulator extends AbstractElementManipulator<GspOuterHtmlElementImpl> {
  @Override
  public GspOuterHtmlElementImpl handleContentChange(@NotNull GspOuterHtmlElementImpl element, @NotNull TextRange range, String newContent) throws IncorrectOperationException {
    String newText = range.replace(element.getText(), newContent);

    CharTable charTable = SharedImplUtil.findCharTableByTree(element.getNode());

    LeafElement e = Factory.createSingleLeafElement(GspTokenTypesEx.GSP_TEMPLATE_DATA, newText, charTable, element.getManager());

    return (GspOuterHtmlElementImpl)element.replace(e.getPsi());
  }
}

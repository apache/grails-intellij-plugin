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

package org.apache.grails.intellij.plugin.lang.gsp.editor.actions;

import com.intellij.codeInsight.editorActions.enter.EnterHandlerDelegate;
import com.intellij.openapi.actionSystem.DataContext;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.EditorModificationUtil;
import com.intellij.openapi.editor.actionSystem.EditorActionHandler;
import com.intellij.openapi.editor.highlighter.EditorHighlighter;
import com.intellij.openapi.editor.highlighter.HighlighterIterator;
import com.intellij.openapi.util.Ref;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.apache.grails.intellij.plugin.lang.gsp.lexer.core.GspTokenTypes;
import org.apache.grails.intellij.plugin.lang.gsp.psi.groovy.api.GspGroovyFile;
import org.apache.grails.intellij.plugin.lang.gsp.psi.gsp.api.GspFile;
import org.jetbrains.plugins.groovy.editor.HandlerUtils;

public final class GspEnterHandler implements EnterHandlerDelegate {

  @Override
  public Result preprocessEnter(@NotNull PsiFile file,
                                @NotNull Editor editor,
                                @NotNull Ref<Integer> caretOffset,
                                @NotNull Ref<Integer> caretAdvance,
                                @NotNull DataContext dataContext,
                                EditorActionHandler originalHandler) {
    if (!(file instanceof GspFile || file instanceof GspGroovyFile)
        || HandlerUtils.isReadOnly(editor)) {
      return Result.Continue;
    }

    int caret = editor.getCaretModel().getOffset();
    if (caret < 1) return Result.Continue;

    final EditorHighlighter highlighter = editor.getHighlighter();
    HighlighterIterator iterator = highlighter.createIterator(caret - 1);
    IElementType tokenType = iterator.getTokenType();

    if (tokenType == GspTokenTypes.JSCRIPT_BEGIN ||
        tokenType == GspTokenTypes.GSCRIPT_BEGIN) {

      boolean isJLike = tokenType == GspTokenTypes.JSCRIPT_BEGIN;

      if (!GspEditorActionsUtil.areSciptletSeparatorsUnbalanced(iterator)) {
        originalHandler.execute(editor, editor.getCaretModel().getCurrentCaret(), dataContext);
      }
      else {
        EditorModificationUtil.insertStringAtCaret(editor, isJLike ? "%>" : "}%");
        editor.getCaretModel().moveCaretRelatively(-2, 0, false, false, true);
        originalHandler.execute(editor, editor.getCaretModel().getCurrentCaret(), dataContext);
        if (isJLike) {
          originalHandler.execute(editor, editor.getCaretModel().getCurrentCaret(), dataContext);
          editor.getCaretModel().moveCaretRelatively(0, -1, false, false, true);
        }
      }
      GspEditorActionsUtil.insertSpacesByGspIndent(editor, file.getProject());
      return Result.Stop;
    }

    return Result.Continue;
  }

}
